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
import java.util.Set;
import java.util.HashSet;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;

import com.example.priceParser.DTO.FlightOffer;
import com.example.priceParser.DTO.FlightSearchResponse;
import com.example.priceParser.util.FlightOfferComparator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


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
            
            if (offers != null && !offers.isEmpty()) {
                offers.sort(new FlightOfferComparator());
                
                Set<String> seenPrices = new HashSet<>();
                offers.removeIf(offer -> !seenPrices.add(offer.getPrice().getGrandTotal()));
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
        headers.setBasicAuth(apiKey, apiSecret); 
        
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
            
            if (response.getBody() != null && response.getBody().get("access_token") != null) {
                String token = (String) response.getBody().get("access_token");
                System.out.println("Получен токен: " + token);
                return token;
            } else {
                throw new RuntimeException("Токен не получен в ответе");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при получении токена: " + e.getMessage());
            throw new RuntimeException("Ошибка при получении токена: " + e.getMessage());
        }
    }

    // No russian airports, -------------------------DEPRECATED-----------------------------------
    @GetMapping("/location")
    public ResponseEntity<?> searchLocations(@RequestParam String keyword) {
        try {
            String accessToken = getAccessToken();
            String searchUrl = baseUrl.replace("v2", "v1") + "/reference-data/locations?keyword=" + keyword + "&subType=CITY,AIRPORT";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(searchUrl, HttpMethod.GET, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null) {
                Object metaObj = responseBody.get("meta");
                if (metaObj instanceof Map) {
                    ((Map) metaObj).remove("links");
                }
            }
            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при поиске локаций: " + e.getMessage());
        }
    }

    @GetMapping("/airports")
    public ResponseEntity<?> getAirports() {
        try {
            String apiUrl = "https://api.travelpayouts.com/data/ru/airports.json";
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при получении списка аэропортов: " + e.getMessage());
        }
    }

    @GetMapping("/cities")
    public ResponseEntity<?> getCities() {
        try {
            String apiUrl = "https://api.travelpayouts.com/data/ru/cities.json";
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при получении списка аэропортов: " + e.getMessage());
        }
    }

    @GetMapping("/countries")
    public ResponseEntity<?> getCountries() {
        try {
            String apiUrl = "https://api.travelpayouts.com/data/ru/countries.json";
            
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при получении списка аэропортов: " + e.getMessage());
        }
    }
    
}