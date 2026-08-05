package com.example.spring3.service;

import com.example.spring3.client.MovieClient;
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
