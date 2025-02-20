package com.example.priceParser.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "airport_codes")
@Data
public class airportCodeEntity {
    @Id
    private String airportCode;
    
    @ManyToOne
    @JoinColumn(name = "city_code")
    private cityCodeEntity cityCode;
    private String country_code;
    private String airport_name;
}
