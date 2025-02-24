package com.example.priceParser.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Column;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.OneToMany;

@Entity
@Table(name = "countries")
@Data
public class CountryCodeEntity {
    @Id
    private String countryCode;
    private String countryName;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> variantNames;
    
    @OneToMany(mappedBy = "country")
    @JsonManagedReference("country-cities")
    private List<CityCodeEntity> cities;
    
    @OneToMany(mappedBy = "country")
    @JsonManagedReference("country-airports")
    private List<AirportCodeEntity> airports;
} 