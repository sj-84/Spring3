package com.example.spring3.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final Map<String, Integer> stock = Map.of(
            "iphone", 10,
            "laptop", 3,
            "headphones", 0
    );

    @GetMapping("/{itemId}")
    public Map<String, Object> checkStock(@PathVariable String itemId) {
        int available = stock.getOrDefault(itemId.toLowerCase(), 0);
        return Map.of(
                "itemId", itemId,
                "available", available,
                "inStock", available > 0
        );
    }
}
