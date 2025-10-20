package com.example.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@SpringBootTest(classes = OrderServiceApplicationTests.TestConfig.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration",
        "spring.data.mongodb.host=localhost",
        "spring.data.mongodb.port=0",
        "spring.data.mongodb.database=orderdb",
        "KAFKA_SASL_USERNAME=test-user",
        "KAFKA_SASL_PASSWORD=test-password",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.properties.security.protocol=SASL_PLAINTEXT",
        "spring.kafka.properties.sasl.mechanism=PLAIN"
})
class OrderServiceApplicationTests {

    @Test
    void contextLoads() {
    }

    @Configuration
    static class TestConfig {

        @Bean
        public MongoTemplate mongoTemplate() {
            String connectionString = String.format("mongodb://%s:%s/%s",
                    "localhost", "0", "orderdb");
            MongoClient mongoClient = MongoClients.create(connectionString);
            return new MongoTemplate(new SimpleMongoClientDatabaseFactory(mongoClient, "orderdb"));
        }
    }
}