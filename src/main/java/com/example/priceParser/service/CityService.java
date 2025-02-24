package com.example.priceParser.service;

import com.example.priceParser.model.CityCodeEntity;
import com.example.priceParser.repository.CityCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {
    
    private final CityCodeRepository cityCodeRepository;
    
    @Transactional(readOnly = true)
    public List<CityCodeEntity> findCitiesWithAirports(String cityName) {
        List<CityCodeEntity> cities = cityCodeRepository.findByVariantNamesContainingIgnoreCase(cityName);
        cities.forEach(city -> city.getAirports().size());
        return cities;
    }
    
    @Transactional(readOnly = true)
    public List<CityCodeEntity> findCitiesByPartialNameWithAirports(String cityName) {
        List<CityCodeEntity> cities = cityCodeRepository.findByPartialName(cityName);
        cities.forEach(city -> city.getAirports().size());
        return cities;
    }
} 