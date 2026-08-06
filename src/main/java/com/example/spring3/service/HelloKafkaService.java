package com.example.spring3.service;

import com.example.spring3.kafka.KafkaPublisher;
import com.example.spring3.model.HelloModel;
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
