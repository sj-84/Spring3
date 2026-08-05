package com.example.spring3.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeatherConfig {

    @Bean
    public WeatherApiClient weatherApiClient(@Value("${app.weather.base-url}") String baseUrl,
                                             @Value("${app.weather.timeout}") int timeoutSeconds) {
        return new WeatherApiClient(baseUrl, timeoutSeconds);
    }
}
