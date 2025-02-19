package com.example.priceParser.model;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;  

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightOffer {
    @Getter
    @JsonProperty("id")
    private String id;
    @Getter
    @JsonProperty("lastTicketingDate")
    private LocalDate lastTicketingDate;
    @Getter
    @JsonProperty("numberOfBookableSeats")
    private int numberOfBookableSeats;
    @Getter
    @JsonProperty("itineraries")
    private List<Itinerary> itineraries;
    @Getter
    @JsonProperty("price")
    private Price price;
    @Getter
    @JsonProperty("travelerPricings")
    private List<TravelerPricing> travelerPricings;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Itinerary {
        @Getter
        @JsonProperty("duration")
        private String duration;
        @Getter
        @JsonProperty("segments")
        private List<Segment> segments;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Segment {
        @Getter
        @JsonProperty("departure")
        private ArrivalDeparture departure;
        @Getter
        @JsonProperty("arrival")
        private ArrivalDeparture arrival;
        @Getter
        @JsonProperty("carrierCode")
        private String carrierCode;
        @Getter
        @JsonProperty("duration")
        private String duration;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArrivalDeparture {
        @Getter
        @JsonProperty("iataCode")
        private String iataCode;
        @Getter
        @JsonProperty("at")
        private String at;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        @Getter
        @JsonProperty("currency")
        private String currency;
        @Getter
        @JsonProperty("grandTotal")
        private String grandTotal;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TravelerPricing {
        @Getter
        @JsonProperty("fareDetailsBySegment")
        private List<FareDetailsBySegment> fareDetailsBySegment;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FareDetailsBySegment {
        @Getter
        @JsonProperty("cabin")
        private String cabin;
        @Getter
        @JsonProperty("includedCheckedBags")
        private IncludedBags includedCheckedBags;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IncludedBags {
        @Getter
        @JsonProperty("quantity")
        private int quantity;
    } 
}