package com.example.priceParser.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.priceParser.model.airportCodeEntity;
import com.example.priceParser.model.cityCodeEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportCodeRepository extends JpaRepository<airportCodeEntity, String> {
    
    Optional<airportCodeEntity> findByAirportCode(String airportCode);
    
    List<airportCodeEntity> findByCity(cityCodeEntity city);
    
    List<airportCodeEntity> findByCityCityCode(String cityCode);
    
    List<airportCodeEntity> findByCountryCode(String countryCode);
    
    List<airportCodeEntity> findByAirportNameContainingIgnoreCase(String name);
} 