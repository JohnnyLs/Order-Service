package com.example.orderservice.integration;

import com.example.orderservice.OrderServiceApplication;
import com.example.orderservice.dto.CreateOrderDto;
import com.example.orderservice.dto.ItemDto;
import com.example.orderservice.dto.OrderResponseDto;
import com.example.orderservice.dto.UpdateStatusDto;
import com.example.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;


@SpringBootTest(classes = OrderServiceApplication.class)
@EmbeddedKafka(partitions = 1, topics = {"orders.events"}, brokerProperties = {"listeners=PLAINTEXT://0.0.0.0:9092"})
class OrderIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private OrderRepository repository;

    @Autowired
    private CacheManager cacheManager;

    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Embedded Mongo auto-config (no Docker)
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/testdb");
        // Mock Redis in-memory (no server)
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> 6379);
        registry.add("app.kafka.topic", () -> "orders.events");
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void fullFlow_CreateGetUpdate() throws Exception {
        // Create
        CreateOrderDto createDto = new CreateOrderDto();
        createDto.setCustomerId("123");
        ItemDto item = new ItemDto();
        item.setSku("SKU1");
        item.setQuantity(1);
        item.setPrice(10.0);
        createDto.setItems(List.of(item));

        String createResponse = mockMvc.perform(MockMvcRequestBuilders.post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        OrderResponseDto created = new ObjectMapper().readValue(createResponse, OrderResponseDto.class);
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // Get again (cache invalidated, should reflect update)
        String updatedResponse = mockMvc.perform(MockMvcRequestBuilders.get("/orders/" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        OrderResponseDto updated = new ObjectMapper().readValue(updatedResponse, OrderResponseDto.class);
        assertEquals("DELIVERED", updated.getStatus());

        // Verify persisted
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void health_Endpoint() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/orders/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("OK")));
    }
}