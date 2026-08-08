package com.example.ecom1.demo;

public class WeatherApiClient {

    private final String baseUrl;
    private final int timeoutSeconds;

    public WeatherApiClient(String baseUrl, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    public double fetchTemperature(String city) {
        System.out.println("[GET " + baseUrl + "/current?city=" + city + " (timeout " + timeoutSeconds + "s)]");
        return 19.5;
    }
}
