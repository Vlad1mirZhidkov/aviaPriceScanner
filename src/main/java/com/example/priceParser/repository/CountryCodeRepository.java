package com.example.priceParser.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.priceParser.model.CountryCodeEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryCodeRepository extends JpaRepository<CountryCodeEntity, String> {
    
    Optional<CountryCodeEntity> findByCountryCode(String countryCode);
    
    @Query(value = "SELECT * FROM countries WHERE variant_names @> :name::jsonb", nativeQuery = true)
    Optional<CountryCodeEntity> findByVariantName(@Param("name") String name);
    
    @Query(value = "SELECT * FROM countries WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(variant_names) " +
           "WHERE value ILIKE :pattern)", nativeQuery = true)
    List<CountryCodeEntity> findByNamePattern(@Param("pattern") String pattern);
} 