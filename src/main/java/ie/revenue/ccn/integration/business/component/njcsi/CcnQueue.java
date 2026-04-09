package ie.revenue.ccn.integration.business.component.njcsi;

import ie.revenue.ccn.integration.exceptions.CcnConnectionException;
import ie.revenue.ccn.integration.external.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.Callable;

@Slf4j
public class CcnQueue {

    @Getter
    private final String queueName;

    private final String replyToQueueName;
    private final String replyToQueueDestination;
    private final int browseSize;
    private final CcnMessageReceiver messageReceiver;

    private static final String RESULT = "result";
    private static final String APPLICATION = "application";
    private static final String QUEUE = "queue";

    @Getter
    private int currentQueueDepth = 0;

    @Getter
    private Instant lastBrowseTime = Instant.EPOCH;

    private final Timer successfulSendTimer;
    private final Timer failedSendTimer;
    private final Timer successfulReceiveTimer;
    private final Timer failedReceiveTimer;

    public CcnQueue(String queueName,
                    String owningApplicationName,
                    String replyToQueueName,
                    String replyToQueueDestination,
                    int browseSize,
                    CcnMessageReceiver messageReceiver,
                    MeterRegistry meterRegistry) {

        this.queueName = queueName;
        this.replyToQueueName = replyToQueueName;
        this.replyToQueueDestination = replyToQueueDestination;
        this.browseSize = browseSize;
        this.messageReceiver = messageReceiver;

        this.successfulSendTimer = Timer.builder("revenue.ccnintegration.outbound.messages")
                .description("Time taken to send messages to CCN/CSI queues")
                .tag(APPLICATION, owningApplicationName)
                .tag(QUEUE, queueName)
                .tag(RESULT, "success")
                .register(meterRegistry);

        this.failedSendTimer = Timer.builder("revenue.ccnintegration.outbound.messages")
                .description("Time taken to send messages to CCN/CSI queues")
                .tag(APPLICATION, owningApplicationName)
                .tag(QUEUE, queueName)
                .tag(RESULT, "failure")
                .register(meterRegistry);

        this.successfulReceiveTimer = Timer.builder("revenue.ccnintegration.inbound.messages")
                .description("Time taken to process messages received from CCN/CSI queues")
                .tag(APPLICATION, owningApplicationName)
                .tag(QUEUE, queueName)
                .tag(RESULT, "success")
                .register(meterRegistry);

        this.failedReceiveTimer = Timer.builder("revenue.ccnintegration.inbound.messages")
                .description("Time taken to process messages received from CCN/CSI queues")
                .tag(APPLICATION, owningApplicationName)
                .tag(QUEUE, queueName)
                .tag(RESULT, "failure")
                .register(meterRegistry);

        Gauge.builder("revenue.ccnintegration.inbound.queuedepth", this, q -> q.currentQueueDepth)
                .description("Current depth of CCN/CSI queue")
                .tag(APPLICATION, owningApplicationName)
                .tag(QUEUE, queueName)
                .register(meterRegistry);

        Gauge.builder("revenue.ccnintegration.inbound.queue.lastbrowse.timestamp",
                        this, q -> q.lastBrowseTime.getEpochSecond())
                .description("Last time the queue was browsed")
                .tag(APPLICATION, owningApplicationName)
                .tag(QUEUE, queueName)
                .baseUnit("seconds")
                .register(meterRegistry);
    }

    public MessageAndCorrelationId send(CcnConnection connection,
                                        String destination,
                                        String messageType,
                                        String messageBody,
                                        String messageId,
                                        String correlationId) {

        return time(() ->
                        connection.doWithConnection(conn -> {
                            try {
                                NjcsiQueue csiQueue = conn.createQueue(queueName, true);

                                try (NjcsiQueueSender sender = csiQueue.createSender()) {
                                    log.info("Sending message to queue {} with destination {} and message type {}",
                                            queueName, destination, messageType);

                                    sender.passMessageID(true);
                                    sender.passCorrelationID(true);
                                    sender.setReplyToQueue(replyToQueueName,
                                            new NjcsiDestination(replyToQueueDestination));

                                    NjcsiEnqueuedMessage message =
                                            buildEnqueuedMessage(messageType, messageBody, messageId, correlationId, destination);

                                    sender.send(message);
                                    return buildResponse(message);
                                }
                            } catch (Exception e) {
                                log.error("Error occurred while sending message to queue {}", queueName, e);
                                throw new CcnConnectionException("Error sending message to queue " + queueName, e);
                            }
                        }),
                successfulSendTimer,
                failedSendTimer
        );
    }

    private Object buildResponse(NjcsiEnqueuedMessage message) {
    }

    private NjcsiEnqueuedMessage buildEnqueuedMessage(String messageType, String messageBody, String messageId, String correlationId, String destination) {
    }

    public int checkDepth(CcnConnection connection) {
        return connection.doWithConnection(conn -> {
            try {
                NjcsiQueue csiQueue = conn.createQueue(queueName, true);
                int depth = csiQueue.getDepth();
                log.info("CSI_QUEUE=[{}] DEPTH=[{}]", queueName, depth);
                return depth;
            } catch (Exception e) {
                log.error("Error checking depth of queue {}", queueName, e);
                throw new CcnConnectionException("Error checking depth of queue " + queueName, e);
            }
        });
    }

    public void browse(CcnConnection connection) {
        connection.doWithConnection(conn -> {
            try {
                NjcsiQueue csiQueue = conn.createQueue(queueName, true);

                try (NjcsiQueueBrowser browser = csiQueue.createBrowser()) {

                    int count = 0;
                    boolean completed = false;

                    this.currentQueueDepth = csiQueue.getDepth();
                    this.lastBrowseTime = Instant.now();

                    log.info("CSI_QUEUE=[{}] DEPTH=[{}]", queueName, currentQueueDepth);

                    while (!completed && currentQueueDepth > 0) {

                        NjcsiDequeuedMessage csiMessage = browser.browse(1000, true);

                        if (csiMessage == null) {
                            log.info("No more messages available on queue: {}", queueName);
                            completed = true;
                        } else if (NjcsiQueueMessageType.REPORT == csiMessage.getQueueMessageType()) {
                            handleReportMessage(csiMessage);
                            browser.delete();
                        } else {
                            logMessageDetails(csiMessage);

                            consumeReceivedMessage(
                                    new String(csiMessage.getData().getBytes(), StandardCharsets.UTF_8),
                                    Base64.getEncoder().encodeToString(csiMessage.getMessageID()),
                                    Base64.getEncoder().encodeToString(csiMessage.getCorrelationID())
                            );

                            browser.delete();
                        }

                        if (count++ >= browseSize && browseSize != -1) {
                            log.info("Browse size limit reached: {}", browseSize);
                            completed = true;
                        }
                    }
                }
                return null;
            } catch (Exception e) {
                log.error("Error occurred while browsing queue {}", queueName, e);
                throw new CcnConnectionException("Error browsing queue " + queueName, e);
            }
        });
    }

    protected void consumeReceivedMessage(String messageBody, String messageId, String correlationId) {
        time(() -> {
            messageReceiver.receive(messageBody, messageId, correlationId);
            return null;
        }, successfulReceiveTimer, failedReceiveTimer);
    }

    private void logMessageDetails(NjcsiDequeuedMessage csiMessage) {
        try {
            String reportType = NjcsiQueueMessageType.REPORT == csiMessage.getQueueMessageType()
                    ? " - ReportType=" + csiMessage.getReportType()
                    : "";

            log.info("Message Details: QueueMessageType={} - MessageType={} - MessageID={} - CorrelationID={}{}",
                    csiMessage.getQueueMessageType(),
                    csiMessage.getMessageType(),
                    csiMessage.getMessageID(),
                    csiMessage.getCorrelationID(),
                    reportType);

        } catch (Exception e) {
            log.warn("Error logging message details", e);
        }
    }

    private void handleReportMessage(NjcsiDequeuedMessage csiMessage) {
        try {
            log.info("Processing REPORT message from queue: {} - ReportType: {}",
                    queueName,
                    csiMessage.getReportType());
        } catch (Exception e) {
            log.error("Error handling report message", e);
        }
    }

    protected <T> T time(Callable<T> callable, Timer successTimer, Timer failureTimer) {
        Timer.Sample sample = Timer.start();
        try {
            T result = callable.call();
            sample.stop(successTimer);
            return result;
        } catch (Exception e) {
            sample.stop(failureTimer);
            throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
        }
    }
}