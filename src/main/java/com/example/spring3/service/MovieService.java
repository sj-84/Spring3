package com.example.spring3.service;

import com.example.spring3.client.MovieClient;
import org.springframework.stereotype.Component;

@Component
public class MovieService {
    MovieClient movieClient;
    MovieService(MovieClient movieClient) {
        this.movieClient = movieClient;
    }

   public String recommend(String genre) {
        int x = this.movieClient.fetchPopularity(genre);
        return String.valueOf(x);
    }
}
