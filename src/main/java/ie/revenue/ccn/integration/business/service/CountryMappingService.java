package ie.revenue.ccn.integration.business.service;

import ie.revenue.ccn.integration.exceptions.CountryNotFoundException;

import java.util.Map;

public class CountryMappingService {

    private final Map<String, Map<String, String>> applicationToCountryToDestinationMap;

    public CountryMappingService(Map<String, Map<String, String>> applicationToCountryToDestinationMap) {
        this.applicationToCountryToDestinationMap = applicationToCountryToDestinationMap;
    }

    public String getDestinationByApplicationTypeAndCountry(String applicationType, String countryCode) {

        Map<String, String> countryToDestination =
                applicationToCountryToDestinationMap.get(applicationType);

        if (countryToDestination == null) {
            throw new CountryNotFoundException(
                    "No country mapping found for application: " + applicationType
            );
        }

        String destination = countryToDestination.get(countryCode);

        if (destination == null) {
            throw new CountryNotFoundException(
                    "No destination found for country code: " + countryCode +
                    " in application: " + applicationType
            );
        }

        return destination;
    }

    public Map<String, String> getCountryToDestinationMapByApplicationType(String applicationType) {

        Map<String, String> countryToDestination =
                applicationToCountryToDestinationMap.get(applicationType);

        if (countryToDestination == null) {
            throw new CountryNotFoundException(
                    "No country mapping found for application: " + applicationType
            );
        }

        return countryToDestination;
    }
}
