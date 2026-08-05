package com.example.spring3.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoRunner implements CommandLineRunner { //what is CommandLineRunner

    private final WeatherService weatherService;

    DemoRunner(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @Override
    public void run(String... args) {
        System.out.println(weatherService.report("Tokyo"));
    }
}
