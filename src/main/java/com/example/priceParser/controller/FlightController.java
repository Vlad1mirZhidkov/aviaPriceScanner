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

    private final AirportCodeRepository airportCodeRepository;
    
    private final CityCodeRepository cityCodeRepository;


    @GetMapping("/search-flights")
    public ResponseEntity<?> test(@RequestParam String nameDepartureCity, @RequestParam String nameDestinationCity, @RequestParam String departureDate, @RequestParam(required = false, defaultValue = "RUB") String currencyCode) {
        List<CityCodeEntity> departureCities = cityCodeRepository.findByVariantNamesContainingIgnoreCase(nameDepartureCity);
        List<CityCodeEntity> destinationCities = cityCodeRepository.findByVariantNamesContainingIgnoreCase(nameDestinationCity);
        
        if (!departureCities.isEmpty() && !destinationCities.isEmpty()) {
            CityCodeEntity departureCity = departureCities.get(0);
            CityCodeEntity destinationCity = destinationCities.get(0);
            
            List<AirportCodeEntity> departureAirports = departureCity.getAirports();
            List<AirportCodeEntity> destinationAirports = destinationCity.getAirports();
            
            List<FlightOffer> flightOffers = new ArrayList<>();

            if (!departureAirports.isEmpty() && !destinationAirports.isEmpty()) {
                List<String> departureCodes = departureAirports.stream()
                    .map(AirportCodeEntity::getAirportCode)
                    .collect(Collectors.toList());
                List<String> destinationCodes = destinationAirports.stream()
                    .map(AirportCodeEntity::getAirportCode)
                    .collect(Collectors.toList());
                for (String departureCode : departureCodes) {
                    for (String destinationCode : destinationCodes) {
                        ResponseEntity<?> result = searchFlights(departureCode, destinationCode, departureDate, currencyCode);
                        if (result.getBody() != null) {
                            flightOffers.addAll((List<FlightOffer>) result.getBody());
                        }
                    }
                }
                
                flightOffers.sort((o1, o2) -> {
                    Double price1 = Double.parseDouble(o1.getPrice().getGrandTotal());
                    Double price2 = Double.parseDouble(o2.getPrice().getGrandTotal());
                    
                    int priceCompare = price1.compareTo(price2);
                    if (priceCompare != 0) {
                        return priceCompare;
                    }
                    
                    int duration1 = parseDuration(o1.getItineraries().get(0).getDuration());
                    int duration2 = parseDuration(o2.getItineraries().get(0).getDuration());
                    return Integer.compare(duration1, duration2);
                });
                
                Map<Double, FlightOffer> uniqueOffers = new LinkedHashMap<>();
                for (FlightOffer offer : flightOffers) {
                    Double price = Double.parseDouble(offer.getPrice().getGrandTotal());
                    if (!uniqueOffers.containsKey(price) || 
                        parseDuration(offer.getItineraries().get(0).getDuration()) < 
                        parseDuration(uniqueOffers.get(price).getItineraries().get(0).getDuration())) {
                        uniqueOffers.put(price, offer);
                    }
                }
                
                return ResponseEntity.ok(new ArrayList<>(uniqueOffers.values()));
            }
            return ResponseEntity.ok(flightOffers);
        }
        return ResponseEntity.ok("No airports found");
    }
    
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
        // Предполагаем, что длительность в формате "PT2H30M" (2 часа 30 минут)
        duration = duration.substring(2); // Убираем "PT"
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
        
        return hours * 60 + minutes; // Конвертируем всё в минуты
    }
}