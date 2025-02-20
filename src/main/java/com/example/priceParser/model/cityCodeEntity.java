package com.example.priceParser.model;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Type;
import com.vladmihalcea.hibernate.type.json.JsonType;


@Entity
@Table(name= "city_code")
@Data
public class cityCodeEntity {
    @Id
    private String city_code;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> variant_names;
    private String country_code;
}
