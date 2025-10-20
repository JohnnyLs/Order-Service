package com.example.orderservice.integration;

import com.example.orderservice.OrderServiceApplication;
import com.example.orderservice.dto.CreateOrderDto;
import com.example.orderservice.dto.OrderResponseDto;
import com.example.orderservice.dto.UpdateStatusDto;
import com.example.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OrderServiceApplication.class)
@Testcontainers
@EmbeddedKafka(partitions = 1, topics = {"orders.events"})
class OrderIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDB = new MongoDBContainer("mongo:7.0");

    @Container
    @ServiceConnection
    static RedisContainer redis = new RedisContainer("redis:7.2");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OrderRepository repository;

    @Autowired
    private CacheManager cacheManager;

    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDB::getReplicaSetUrl);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        registry.add("app.kafka.topic", () -> "orders.events");
    }

    @Test
    void fullFlow_CreateGetUpdate() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // Create
        CreateOrderDto createDto = new CreateOrderDto();
        createDto.setCustomerId("123");
        createDto.setItems(List.of(new com.example.orderservice.dto.ItemDto("SKU1", 1, 10.0)));

        String createResponse = mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderResponseDto created = new com.fasterxml.jackson.databind.ObjectMapper().readValue(createResponse, OrderResponseDto.class);
        String id = created.getId();
        assertEquals("NEW", created.getStatus());

        // Get (should cache)
        mockMvc.perform(MockMvcRequestBuilders.get("/orders/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"));

        // Update status
        UpdateStatusDto updateDto = new UpdateStatusDto();
        updateDto.setStatus("DELIVERED");

        mockMvc.perform(MockMvcRequestBuilders.patch("/orders/" + id + "/status")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // Get again (cache invalidated, should reflect update)
        String updatedResponse = mockMvc.perform(MockMvcRequestBuilders.get("/orders/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        OrderResponseDto updated = new com.fasterxml.jackson.databind.ObjectMapper().readValue(updatedResponse, OrderResponseDto.class);
        assertEquals("DELIVERED", updated.getStatus());

        // Verify persisted
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void health_Endpoint() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        mockMvc.perform(MockMvcRequestBuilders.get("/orders/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());  // String response
    }
}