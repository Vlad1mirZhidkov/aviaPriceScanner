package com.example.priceParser.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.priceParser.model.AirportCodeEntity;
import com.example.priceParser.model.CityCodeEntity;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


@Repository
public interface AirportCodeRepository extends JpaRepository<AirportCodeEntity, String> {
    
    Optional<AirportCodeEntity> findByAirportCode(String airportCode);
    
    List<AirportCodeEntity> findByCity(CityCodeEntity city);
    
    List<AirportCodeEntity> findByCityCityCode(String cityCode);
    
    List<AirportCodeEntity> findByCountry_CountryCode(String countryCode);
    
    List<AirportCodeEntity> findByAirportNameContainingIgnoreCase(String name);

    @Query(value = "SELECT * FROM airport_codes WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(variant_names) " +
           "WHERE value ILIKE CONCAT('%', :name, '%'))", nativeQuery = true)
    List<AirportCodeEntity> findByVariantNamesContainingIgnoreCase(@Param("name") String name);
} 