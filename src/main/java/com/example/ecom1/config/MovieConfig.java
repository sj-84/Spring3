package com.example.ecom1.config;

import com.example.ecom1.client.MovieClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MovieConfig {

    @Bean
    public MovieClient movieClient(@Value("${app.movie.base.url}") String baseUrl, @Value("${app.movie.apiKey}") String apiKey) {
        return new MovieClient(baseUrl, apiKey);
    }
}
