package com.example.priceParser.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import com.example.priceParser.model.FlightSearchResponse;
import com.example.priceParser.model.FlightOffer;
import com.example.priceParser.util.FlightOfferComparator;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {
    
    @Value("${amadeus.api.key}")
    private String apiKey;
    
    @Value("${amadeus.api.secret}")
    private String apiSecret;
    
    @Value("${amadeus.api.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    
    @GetMapping("/search")
    public ResponseEntity<?> searchFlights(
            @RequestParam String originLocationCode,
            @RequestParam String destinationLocationCode,
            @RequestParam String departureDate,
            @RequestParam(required = false, defaultValue = "RUB") String currencyCode
    ) {
        try {
            String accessToken = getAccessToken();
            String searchUrl = baseUrl + "/shopping/flight-offers";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> originDestination = new HashMap<>();
            originDestination.put("id", "1");
            originDestination.put("originLocationCode", originLocationCode);
            originDestination.put("destinationLocationCode", destinationLocationCode);
            Map<String, String> dateRange = new HashMap<>();
            dateRange.put("date", departureDate);
            originDestination.put("departureDateTimeRange", dateRange);
            requestBody.put("originDestinations", List.of(originDestination));
            
            Map<String, Object> traveler = new HashMap<>();
            traveler.put("id", "1");
            traveler.put("travelerType", "ADULT");
            requestBody.put("travelers", List.of(traveler));
            
            requestBody.put("sources", List.of("GDS"));
            
            requestBody.put("searchCriteria", new HashMap<String, Object>() {{
                put("maxFlightOffers", 20);
                put("flightFilters", new HashMap<String, Object>());
            }});
            
            requestBody.put("currencyCode", currencyCode);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<FlightSearchResponse> response = restTemplate.postForEntity(
                searchUrl, 
                request, 
                FlightSearchResponse.class
            );
            
            List<FlightOffer> offers = response.getBody().getData();
            
            // Сортируем предложения
            if (offers != null && !offers.isEmpty()) {
                offers.sort(new FlightOfferComparator());
            }
            
            return ResponseEntity.ok(offers);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при поиске рейсов: " + e.getMessage());
        }
    }
    
    private String getAccessToken() {
        String tokenUrl = "https://test.api.amadeus.com/v1/security/oauth2/token";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("client_id", apiKey);
        params.add("client_secret", apiSecret);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
        
        return (String) response.getBody().get("access_token");
    }
} 