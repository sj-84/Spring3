package com.example.spring3.Runners;

import com.example.spring3.service.MovieService;
import org.springframework.boot.CommandLineRunner;

public class MovieRunner implements CommandLineRunner {

    MovieService movieService;

    MovieRunner(MovieService movieService) {
        this.movieService = movieService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(this.movieService.recommend("Sci-Fi"));
    }
}
