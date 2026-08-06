package com.example.spring3.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String GREETING_TOPIC = "greetings.created";

    @Bean
    public NewTopic greetings() {
        return TopicBuilder.name(GREETING_TOPIC).partitions(1)
                .replicas(1)
                .build();
    }

}
