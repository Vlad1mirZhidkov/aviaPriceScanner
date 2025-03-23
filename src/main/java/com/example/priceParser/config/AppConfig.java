package com.example.priceParser.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Value("${spring.rest.template.connection-timeout:10000}")
    private int connectionTimeout;

    @Value("${spring.rest.template.read-timeout:30000}")
    private int readTimeout;
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        ClientHttpRequestFactory factory = new BufferingClientHttpRequestFactory(clientHttpRequestFactory());
        
        RestTemplate restTemplate = builder
            .setConnectTimeout(Duration.ofMillis(connectionTimeout))
            .setReadTimeout(Duration.ofMillis(readTimeout))
            .build();
        
        restTemplate.setRequestFactory(factory);
        
        // Добавляем интерцептор для логирования запросов и ответов
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            log.debug("HTTP Request: {} {}", request.getMethod(), request.getURI());
            log.trace("Request Headers: {}", request.getHeaders());
            
            // Выполняем запрос
            var response = execution.execute(request, body);
            
            log.debug("HTTP Response: {} {}", response.getStatusCode(), request.getURI());
            log.trace("Response Headers: {}", response.getHeaders());
            
            return response;
        });
        
        restTemplate.setInterceptors(interceptors);
        
        return restTemplate;
    }
    
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        factory.setBufferRequestBody(false); // Отключаем буферизацию для экономии памяти
        return factory;
    }
} 