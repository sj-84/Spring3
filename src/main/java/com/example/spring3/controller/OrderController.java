package com.example.spring3.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final RestClient restClient;

    public OrderController(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://localhost:8080").build();
    }

    @GetMapping("/{itemId}")
    public Map<String, Object> placeOrder(@PathVariable String itemId) {
        Map<String, Object> inventory = restClient.get()
                .uri("/api/inventory/{itemId}", itemId)
                .retrieve()
                .body(Map.class);

        int available = ((Number) inventory.get("available")).intValue();

        if (available == 0) {
            return Map.of(
                    "itemId", itemId,
                    "status", "OUT_OF_STOCK",
                    "message", "Cannot place order, item is unavailable"
            );
        }

        return Map.of(
                "itemId", itemId,
                "status", "PLACED",
                "message", "Order placed successfully",
                "unitsAvailable", available
        );
    }
}
