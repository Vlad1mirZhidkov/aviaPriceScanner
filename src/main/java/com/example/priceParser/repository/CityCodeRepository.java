package com.example.priceParser.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.priceParser.model.cityCodeEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityCodeRepository extends JpaRepository<cityCodeEntity, String> {
    
    Optional<cityCodeEntity> findByCityCode(String cityCode);
    
    @Query(value = "SELECT * FROM cities WHERE variant_names @> :name::jsonb", nativeQuery = true)
    Optional<cityCodeEntity> findByVariantName(@Param("name") String name);
    
    List<cityCodeEntity> findByCountryCode(String countryCode);
    
    @Query(value = "SELECT * FROM cities WHERE EXISTS (SELECT 1 FROM jsonb_array_elements_text(variant_names) " +
           "WHERE value ILIKE :pattern)", nativeQuery = true)
    List<cityCodeEntity> findByNamePattern(@Param("pattern") String pattern);
} 