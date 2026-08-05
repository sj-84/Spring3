package com.example.spring3.client;

public class MovieClient {

    String baseUrl;
    String apiKey;
    public MovieClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    public int fetchPopularity(String title) {
        System.out.println("[GET " + baseUrl + "/current?movie=" + title + "/apiKey=" + apiKey + ")]");
        return 85;
    }
}
