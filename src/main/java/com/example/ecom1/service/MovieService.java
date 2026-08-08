package com.example.ecom1.service;

import com.example.ecom1.client.MovieClient;
import org.springframework.stereotype.Component;

@Component
public class MovieService {
    private final MovieClient movieClient;

    MovieService(MovieClient movieClient) {
        this.movieClient = movieClient;
    }

    public String recommend(String genre) {
        int popularity = movieClient.fetchPopularity(genre);
        return "Recommendation for " + genre + ": popularity score " + popularity + "/100";
    }
}
