package com.example.priceParser.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Data;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "cities")
@Data
public class CityCodeEntity {
    @Id
    private String cityCode;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    @ElementCollection
    private List<String> variantNames;
    
    @ManyToOne
    @JoinColumn(name = "country_code")
    @JsonBackReference("country-cities")
    private CountryCodeEntity country;
    
    @OneToMany(mappedBy = "city")
    @JsonManagedReference("city-airports")
    private List<AirportCodeEntity> airports;
}
