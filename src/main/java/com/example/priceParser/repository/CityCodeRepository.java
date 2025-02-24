package com.example.priceParser.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.priceParser.model.CityCodeEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityCodeRepository extends JpaRepository<CityCodeEntity, String> {
    
    Optional<CityCodeEntity> findByCityCode(String cityCode);
    
    @Query(value = "SELECT * FROM cities WHERE variant_names @> :name::jsonb", nativeQuery = true)
    Optional<CityCodeEntity> findByVariantName(@Param("name") String name);
    
    List<CityCodeEntity> findByCountry_CountryCode(String code);
    
    @Query(value = "SELECT * FROM cities WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(variant_names) " +
           "WHERE value ILIKE :pattern)", nativeQuery = true)
    List<CityCodeEntity> findByNamePattern(@Param("pattern") String pattern);
} 