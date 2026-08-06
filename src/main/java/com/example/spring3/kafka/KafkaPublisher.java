package com.example.spring3.kafka;

import com.example.spring3.config.KafkaConfig;
import com.example.spring3.model.HelloModel;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
public class KafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(HelloModel helloModel) {
        kafkaTemplate.send(KafkaConfig.GREETING_TOPIC, "id1", helloModel.getName());
    }
}

//=================================================================================
// LINE-BY-LINE EXPLANATION (line numbers refer to the code above, before these comments)
//=================================================================================

// Line 1: package com.example.spring3.kafka;
// Declares that this class lives in the "kafka" folder. "package" is how Java
// groups related classes and avoids name collisions.

// Line 3: import com.example.spring3.config.KafkaConfig;
// Brings in KafkaConfig so we can use KafkaConfig.GREETING_TOPIC — the topic
// name constant defined there ("greetings.created").

// Line 4: import com.example.spring3.model.HelloModel;
// Brings in HelloModel, the message payload type this publisher sends.

// Line 5: import org.springframework.kafka.core.KafkaTemplate;
// KafkaTemplate is Spring's ready-made class for sending messages to Kafka.
// You give it the topic and the data, and it handles connecting/batching/serializing.

// Line 6: import org.springframework.stereotype.Component;
// @Component marks this class as a Spring-managed bean.

// Line 9: @Component
// Tells Spring: "create one instance of this class and keep it in the app context
// so other classes can inject it." Without it, the constructor parameter below
// could never be filled.

// Line 10: public class KafkaPublisher {
// Opens the class. "public" = any other class can use it. This is the SENDER /
// producer side of Kafka: it pushes messages onto a topic.

// Line 12: private final KafkaTemplate<String, HelloModel> kafkaTemplate;
// A field holding the template. The two type parameters mean:
//   <String, HelloModel>
//   K = String     -> the message KEY is a String
//   V = HelloModel -> the message VALUE (payload) is a HelloModel
// private = only this class can see it. final = set once in the constructor.

// Line 14: public KafkaPublisher(KafkaTemplate<String, HelloModel> kafkaTemplate) {
// The constructor. Spring sees a single parameter (the template) and injects the
// bean it already created for KafkaTemplate. This is called constructor injection.

// Line 15: this.kafkaTemplate = kafkaTemplate;
// Saves the injected template into the field, so other methods can use it.

// Line 18: public void sendMessage(HelloModel helloModel) {
// Public method. Callers give it a HelloModel; it sends that model to Kafka.
// "void" = returns nothing.

// Line 19: kafkaTemplate.send(KafkaConfig.GREETING_TOPIC, "id1", helloModel);
// This is the actual send. Three arguments, in order:
//   1) KafkaConfig.GREETING_TOPIC -> "greetings.created" (the topic, a String)
//   2) "id1"                      -> the KEY (a String, matching K = String)
//   3) helloModel                 -> the VALUE/payload (a HelloModel, matching V)
// send(topic, key, data) is the 3-arg overload that fits this template.

// Line 20: }
// Closes sendMessage.

// Line 21: }
// Closes the KafkaPublisher class.

// WHY SEND TAKES THE WHOLE MODEL AS THE 3RD PARAMETER:
// A Kafka message has two parts:
//   key   -> a label (optional). Used for partitioning/ordering. Same key lands on
//            the same partition.
//   value -> the actual content / payload. THIS is what the consumer receives.
// The template is typed KafkaTemplate<String, HelloModel>, so the value MUST be a
// HelloModel. Sending helloModel.getName() would fail because getName() returns a
// String, not a HelloModel — Java checks the type at compile time.

// HOW IT ALL FITS TOGETHER:
// 1) Someone calls sendMessage(new HelloModel("Batman")).
// 2) send() pushes the message (key "id1", value the HelloModel) onto the topic.
// 3) Any Kafka consumer subscribed to "greetings.created" receives that HelloModel.
