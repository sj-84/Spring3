package com.example.ecom1.runners;

import com.example.ecom1.service.MovieService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MovieRunner implements CommandLineRunner {

    private final MovieService movieService;

    MovieRunner(MovieService movieService) {
        this.movieService = movieService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(movieService.recommend("Sci-Fi"));
    }
}
