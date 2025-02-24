package com.example.priceParser.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.priceParser.model.CityCodeEntity;
import com.example.priceParser.model.CountryCodeEntity;
import com.example.priceParser.model.AirportCodeEntity;
import com.example.priceParser.repository.AirportCodeRepository;
import com.example.priceParser.repository.CityCodeRepository;
import com.example.priceParser.repository.CountryCodeRepository;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpMethod;
import org.springframework.core.ParameterizedTypeReference;
import lombok.extern.slf4j.Slf4j;
import java.util.stream.Collectors;


import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;


@Service
@Slf4j
@Transactional
public class DataParsingService {
    private final RestTemplate restTemplate;
    private final CountryCodeRepository countryRepository;
    private final CityCodeRepository cityRepository;
    private final AirportCodeRepository airportRepository;
    private Map<String, Map<String, List<Map<String, Object>>>> groupedData;

    @Value("${airports.api.url}")
    private String airportsApiUrl;

    public DataParsingService(
        RestTemplate restTemplate,
        CountryCodeRepository countryRepository,
        CityCodeRepository cityRepository,
        AirportCodeRepository airportRepository
    ) {
        this.restTemplate = restTemplate;
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
        this.airportRepository = airportRepository;
    }

    public void parseAllData() {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                airportsApiUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (response.getBody() != null) {
                List<Map<String, Object>> airportsData = response.getBody();
                
                Map<String, Map<String, List<Map<String, Object>>>> groupedData = 
                    airportsData.stream()
                        .collect(Collectors.groupingBy(
                            airport -> (String) airport.get("country_code"),
                            Collectors.groupingBy(
                                airport -> (String) airport.get("city_code")
                            )
                        ));

                processGroupedData(groupedData);
                
                log.info("Парсинг данных успешно завершен");
            }
        } catch (Exception e) {
            log.error("Ошибка при парсинге данных", e);
            throw new RuntimeException("Ошибка парсинга", e);
        }
    }

    private void processGroupedData(
        Map<String, Map<String, List<Map<String, Object>>>> groupedData) {
    
    this.groupedData = groupedData; 
    groupedData.forEach((countryCode, citiesData) -> {
        CountryCodeEntity country = getOrCreateCountry(countryCode);
        
        citiesData.forEach((cityCode, airportsData) -> {
            if (airportsData != null && !airportsData.isEmpty()) {
                CityCodeEntity city = getOrCreateCity(cityCode, country, airportsData);
                
                airportsData.forEach(airportData -> 
                    createAirport(airportData, city));
            } else {
                log.warn("Нет данных аэропортов для города: " + cityCode);
            }
        });
    });
}

    private CountryCodeEntity getOrCreateCountry(String countryCode) {
        return countryRepository.findByCountryCode(countryCode)
            .orElseGet(() -> {
                CountryCodeEntity country = new CountryCodeEntity();
                country.setCountryCode(countryCode);
                Map<String, List<Map<String, Object>>> citiesData = groupedData.get(countryCode);
                if (citiesData != null && !citiesData.isEmpty()) {
                    Map<String, Object> firstAirport = citiesData.values().iterator().next().get(0);
                    String countryName = (String) firstAirport.get("country_name");
                    country.setCountryName(countryName);
                    country.setVariantNames(new ArrayList<>(List.of(countryName)));
                } else {
                    country.setVariantNames(new ArrayList<>());
                }
                return countryRepository.save(country);
            });
    }

    private CityCodeEntity getOrCreateCity(
            String cityCode, 
            CountryCodeEntity country, 
            List<Map<String, Object>> airportsData) {
        
        return cityRepository.findByCityCode(cityCode)
            .orElseGet(() -> {
                CityCodeEntity city = new CityCodeEntity();
                city.setCityCode(cityCode);
                city.setCountry(country);
                
                String cityName = (String) airportsData.get(0).get("city_name");
                city.setVariantNames(List.of(cityName));  
                
                return cityRepository.save(city);
            });
    }

    private void createAirport(Map<String, Object> airportData, CityCodeEntity city) {
        String airportCode = (String) airportData.get("code");
        
        if (!airportRepository.existsById(airportCode)) {
            AirportCodeEntity airport = new AirportCodeEntity();
            airport.setAirportCode(airportCode);
            airport.setCity(city);
            airport.setCountry(city.getCountry());
            airport.setAirportName((String) airportData.get("name"));
            
            airportRepository.save(airport);
        }
    }
}