package com.example.ecom1.client;

public class MovieClient {

    private final String baseUrl;
    private final String apiKey;

    public MovieClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public int fetchPopularity(String movie) {
        System.out.println("[GET " + baseUrl + "/current?movie=" + movie + "&apiKey=" + apiKey + "]");
        return 85;
    }
}
