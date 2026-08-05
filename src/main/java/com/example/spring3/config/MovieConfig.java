package com.example.spring3.config;

import com.example.spring3.client.MovieClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class MovieConfig {

    @Bean
    public MovieClient movieClient(@Value("${app.movie.base.url}") String baseUrl, @Value("${app.movie.apiKey}") String apiKey) {
        return new MovieClient(baseUrl, apiKey);
    }
}
