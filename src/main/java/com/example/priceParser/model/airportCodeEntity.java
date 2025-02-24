package com.example.priceParser.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Column;

@Entity
@Table(name = "airport_codes")
@Data
public class AirportCodeEntity {
    @Id
    private String airportCode;
    
    @ManyToOne
    @JoinColumn(name = "city_code")
    private CityCodeEntity city;
    
    @ManyToOne
    @JoinColumn(name = "country_code")
    private CountryCodeEntity country;
    
    @Column(name = "airport_name")
    private String airportName;
}
