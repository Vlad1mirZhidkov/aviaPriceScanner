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
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;


@Entity
@Table(name = "cities")
@Data
public class cityCodeEntity {
    @Id
    private String cityCode;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> variantNames;
    
    @ManyToOne
    @JoinColumn(name = "country_code")
    private countryCodeEntity country;
    
    @OneToMany(mappedBy = "city")
    private List<airportCodeEntity> airports;
}
