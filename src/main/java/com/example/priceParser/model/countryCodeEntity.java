package com.example.priceParser.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;

@Entity
@Table(name = "countries")
@Data
public class CountryCodeEntity {
    @Id
    private String countryCode;
    private String countryName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> variantNames;
} 