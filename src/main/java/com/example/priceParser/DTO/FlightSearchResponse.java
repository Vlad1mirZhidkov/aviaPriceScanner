package com.example.priceParser.DTO;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class FlightSearchResponse {
    private List<FlightOffer> data;
    private Map<String, Object> meta;
} 