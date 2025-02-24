package com.example.priceParser.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cities")
@Data
public class CityCodeEntity {
    @Id
    private String cityCode;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> variantNames;
    
    @ManyToOne
    @JoinColumn(name = "country_code")
    private CountryCodeEntity country;
    
    @OneToMany(mappedBy = "city")
    private List<AirportCodeEntity> airports;
}
