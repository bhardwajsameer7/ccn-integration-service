package ie.revenue.ccn.integration.dto;

import jakarta.xml.bind.annotation.*;
import java.math.BigDecimal;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
        name = "",
        propOrder = {
                "queueName",
                "queueDestination",
                "messageType",
                "csiMessageId",
                "csiCorrelationId",
                "replyToQueueName",
                "replyToQueueDestination",
                "messageTimeStamp"
        }
)
@XmlRootElement(name = "CCNCSIData")
public class CCNCSIData {

    @XmlElement(name = "QueueName", required = true)
    protected String queueName;

    @XmlElement(name = "QueueDestination", required = true)
    protected String queueDestination;

    @XmlElement(name = "MessageType", required = true)
    protected String messageType;

    @XmlElement(name = "CsiMessageId")
    protected String csiMessageId;

    @XmlElement(name = "CsiCorrelationId")
    protected String csiCorrelationId;

    @XmlElement(name = "ReplyToQueueName")
    protected String replyToQueueName;

    @XmlElement(name = "ReplyToQueueDestination")
    protected String replyToQueueDestination;

    @XmlElement(name = "MessageTimeStamp")
    protected BigDecimal messageTimeStamp;

    // Getters and Setters

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getQueueDestination() {
        return queueDestination;
    }

    public void setQueueDestination(String queueDestination) {
        this.queueDestination = queueDestination;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getCsiMessageId() {
        return csiMessageId;
    }

    public void setCsiMessageId(String csiMessageId) {
        this.csiMessageId = csiMessageId;
    }

    public String getCsiCorrelationId() {
        return csiCorrelationId;
    }

    public void setCsiCorrelationId(String csiCorrelationId) {
        this.csiCorrelationId = csiCorrelationId;
    }

    public String getReplyToQueueName() {
        return replyToQueueName;
    }

    public void setReplyToQueueName(String replyToQueueName) {
        this.replyToQueueName = replyToQueueName;
    }

    public String getReplyToQueueDestination() {
        return replyToQueueDestination;
    }

    public void setReplyToQueueDestination(String replyToQueueDestination) {
        this.replyToQueueDestination = replyToQueueDestination;
    }

    public BigDecimal getMessageTimeStamp() {
        return messageTimeStamp;
    }

    public void setMessageTimeStamp(BigDecimal messageTimeStamp) {
        this.messageTimeStamp = messageTimeStamp;
    }
}