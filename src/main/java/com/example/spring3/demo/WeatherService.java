package com.example.spring3.demo;

import org.springframework.stereotype.Component;

@Component
public class WeatherService {

    private final WeatherApiClient weatherApiClient;

    WeatherService(WeatherApiClient weatherApiClient) {
        this.weatherApiClient = weatherApiClient;
    }

    public String report(String city) {
        double temp = weatherApiClient.fetchTemperature(city);
        return "Current temperature in " + city + " is " + temp + "°C";
    }
}
