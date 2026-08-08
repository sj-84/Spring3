package com.example.ecom1.service;

import com.example.ecom1.kafka.KafkaPublisher;
import com.example.ecom1.model.HelloModel;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public class HelloKafkaService {
    private final KafkaPublisher kafkaPublisher;

    public HelloKafkaService(KafkaPublisher kafkaPublisher) {
        this.kafkaPublisher = kafkaPublisher;
    }

    public void send() {
        this.kafkaPublisher.sendMessage(new HelloModel("Hello1"));
    }
}
