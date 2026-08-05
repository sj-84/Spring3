package com.example.spring3.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/") //default route
    public Map<String, Integer> home() {
//        Map<String, String> result = Map.of("status", "OK");
//        return result;
        Map<String, Integer> items = new HashMap<String, Integer>();
        items.put("Apples", 50);
        items.put("Bananas", 30);
        items.put("Oranges", 20);
        return items;
    }

    @GetMapping("/api/public")
    public Map<String, String> publicCheck() {
        return Map.of("message", "Public - accessible before login");
    }

    @GetMapping("/api/private")
    public Map<String, String> privateCheck() {
        return Map.of("message", "Private - accessible only after login");
    }
}

//Map.of(...) is a static factory method added in Java 9 that creates an immutable Map in one line.
//•
//Map.of("status", "OK") builds a Map<String, String> with one entry: key "status", value "OK".
//•
//Each argument is a key-value pair (keys come first), so Map.of("a", 1, "b", 2) makes a 2-entry map.
//•
//Unlike new HashMap<>(), the result is unmodifiable (no put), and it infers types automatically (Map.of infers Map<String, String> here).
//In HomeController it's used to return a JSON body {"status":"OK"} from a controller method.