package com.example.priceParser.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.priceParser.model.AirportCodeEntity;
import com.example.priceParser.model.CityCodeEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface AirportCodeRepository extends JpaRepository<AirportCodeEntity, String> {
    
    Optional<AirportCodeEntity> findByAirportCode(String airportCode);
    
    List<AirportCodeEntity> findByCity(CityCodeEntity city);
    
    List<AirportCodeEntity> findByCityCityCode(String cityCode);
    
    List<AirportCodeEntity> findByCountry_CountryCode(String countryCode);
    
    List<AirportCodeEntity> findByAirportNameContainingIgnoreCase(String name);
} 