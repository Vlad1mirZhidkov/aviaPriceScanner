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
import java.util.stream.Collectors;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.transaction.annotation.Transactional;

import com.example.priceParser.DTO.FlightOffer;
import com.example.priceParser.DTO.FlightSearchResponse;
import com.example.priceParser.util.FlightOfferComparator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.priceParser.model.AirportCodeEntity;
import com.example.priceParser.repository.AirportCodeRepository;
import com.example.priceParser.model.CityCodeEntity;
import com.example.priceParser.repository.CityCodeRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.AllArgsConstructor;
import com.example.priceParser.service.CityService;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
@Slf4j
public class FlightController {
    
    @Value("${amadeus.api.key}")
    private String apiKey;
    
    @Value("${amadeus.api.secret}")
    private String apiSecret;
    
    @Value("${amadeus.api.base-url}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;

    private final CityService cityService;

    @Data
    @AllArgsConstructor
    public static class FlightSearchRequest {
        private String originCode;
        private String destinationCode;
        private String departureDate;
        private String currencyCode;
    }

    @Transactional
    @GetMapping("/search-flights")
    public ResponseEntity<?> test(@RequestParam String nameDepartureCity, 
                                @RequestParam String nameDestinationCity, 
                                @RequestParam String departureDate, 
                                @RequestParam(required = false, defaultValue = "RUB") String currencyCode) {
        try {
            log.info("Поиск рейсов: {} -> {}, дата: {}", nameDepartureCity, nameDestinationCity, departureDate);
            
            List<CityCodeEntity> departureCities = cityService.findCitiesWithAirports(nameDepartureCity);
            List<CityCodeEntity> destinationCities = cityService.findCitiesWithAirports(nameDestinationCity);

            if (departureCities.isEmpty() || destinationCities.isEmpty()) {
                if (departureCities.isEmpty()) {
                    departureCities = cityService.findCitiesByPartialNameWithAirports(nameDepartureCity);
                }
                if (destinationCities.isEmpty()) {
                    destinationCities = cityService.findCitiesByPartialNameWithAirports(nameDestinationCity);
                }
            }

            if (departureCities.isEmpty() || destinationCities.isEmpty()) {
                String message = String.format("Города не найдены: %s и/или %s", 
                    departureCities.isEmpty() ? nameDepartureCity : "",
                    destinationCities.isEmpty() ? nameDestinationCity : "");
                log.warn(message);
                return ResponseEntity.badRequest().body(message);
            }

            List<FlightSearchRequest> searchRequests = new ArrayList<>();
            for (CityCodeEntity departureCity : departureCities) {
                for (CityCodeEntity destinationCity : destinationCities) {
                    List<AirportCodeEntity> departureAirports = departureCity.getAirports();
                    List<AirportCodeEntity> destinationAirports = destinationCity.getAirports();

                    if (!departureAirports.isEmpty() && !destinationAirports.isEmpty()) {
                        for (AirportCodeEntity depAirport : departureAirports) {
                            for (AirportCodeEntity destAirport : destinationAirports) {
                                searchRequests.add(new FlightSearchRequest(
                                    depAirport.getAirportCode(),
                                    destAirport.getAirportCode(),
                                    departureDate,
                                    currencyCode
                                ));
                            }
                        }
                    }
                }
            }

            if (searchRequests.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            int maxConcurrentRequests = 5;
            ExecutorService executor = Executors.newFixedThreadPool(maxConcurrentRequests);
            
            try {
                List<CompletableFuture<List<FlightOffer>>> futures = searchRequests.stream()
                    .map(request -> CompletableFuture.supplyAsync(() -> {
                        try {
                            ResponseEntity<FlightSearchResponse> response = searchFlights(
                                request.getOriginCode(),
                                request.getDestinationCode(),
                                request.getDepartureDate(),
                                request.getCurrencyCode()
                            );
                            List<FlightOffer> offers = response.getBody() != null ? response.getBody().getData() : new ArrayList<>();
                            return offers;
                        } catch (Exception e) {
                            log.error("Ошибка при поиске рейса {} -> {}: {}", 
                                request.getOriginCode(), request.getDestinationCode(), e.getMessage());
                            return new ArrayList<FlightOffer>();
                        }
                    }, executor))
                    .collect(Collectors.toList());

                List<FlightOffer> allFlightOffers = futures.stream()
                    .map(future -> {
                        try {
                            return future.get(10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            log.error("Ошибка при получении результатов поиска: {}", e.getMessage());
                            return new ArrayList<FlightOffer>();
                        }
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

                if (allFlightOffers.isEmpty()) {
                    return ResponseEntity.ok(allFlightOffers);
                }

                Map<Double, FlightOffer> uniqueOffers = allFlightOffers.stream()
                    .collect(Collectors.toMap(
                        offer -> Double.parseDouble(offer.getPrice().getGrandTotal()),
                        offer -> offer,
                        (offer1, offer2) -> parseDuration(offer1.getItineraries().get(0).getDuration()) <
                                          parseDuration(offer2.getItineraries().get(0).getDuration()) ? offer1 : offer2,
                        LinkedHashMap::new
                    ));

                List<FlightOffer> sortedOffers = uniqueOffers.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toList());

                log.info("Найдено {} уникальных предложений", sortedOffers.size());
                return ResponseEntity.ok(sortedOffers);

            } finally {
                executor.shutdown();
            }
            
        } catch (Exception e) {
            log.error("Общая ошибка при поиске рейсов: {}", e.getMessage());
            return ResponseEntity.status(500).body("Ошибка при поиске рейсов: " + e.getMessage());
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<FlightSearchResponse> searchFlights(
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
            
            log.info("Поиск рейсов: {} -> {}", originLocationCode, destinationLocationCode);
            ResponseEntity<FlightSearchResponse> response = restTemplate.postForEntity(
                searchUrl, 
                request, 
                FlightSearchResponse.class
            );
            
            if (response.getBody() != null && response.getBody().getData() != null) {
                log.info("Найдено {} предложений для {} -> {}", 
                    response.getBody().getData().size(), 
                    originLocationCode, 
                    destinationLocationCode);
            } else {
                log.warn("Нет предложений для {} -> {}", originLocationCode, destinationLocationCode);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Ошибка при поиске рейсов {} -> {}: {}", 
                originLocationCode, destinationLocationCode, e.getMessage());
            throw new RuntimeException("Ошибка при поиске рейсов: " + e.getMessage());
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

    // -------------------------------------------------DEPRECATED---------------------------------------------------------
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
    
    private int parseDuration(String duration) {
        duration = duration.substring(2);
        int hours = 0;
        int minutes = 0;
        
        int hIndex = duration.indexOf('H');
        if (hIndex != -1) {
            hours = Integer.parseInt(duration.substring(0, hIndex));
            duration = duration.substring(hIndex + 1);
        }
        
        int mIndex = duration.indexOf('M');
        if (mIndex != -1) {
            minutes = Integer.parseInt(duration.substring(0, mIndex));
        }
        
        return hours * 60 + minutes;
    }
}