package com.example.priceParser.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;

import java.util.List;

@Entity
@Table(name = "countries")
@Data
public class countryCodeEntity {
    @Id
    private String countryCode;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> variantNames;  
    
    @OneToMany(mappedBy = "country")
    private List<cityCodeEntity> cities;
} 