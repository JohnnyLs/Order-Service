package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderDto;
import com.example.orderservice.dto.ItemDto;
import com.example.orderservice.dto.OrderResponseDto;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_Success() throws Exception {
        CreateOrderDto dto = new CreateOrderDto();
        dto.setCustomerId("123");
        ItemDto item = new ItemDto();
        item.setSku("SKU1");
        item.setQuantity(1);
        item.setPrice(10.0);
        dto.setItems(List.of(item));


        OrderResponseDto response = new OrderResponseDto();
        response.setId("test-id");
        when(service.create(any(CreateOrderDto.class))).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id"));
    }

    @Test
    void getById_Success() throws Exception {
        OrderResponseDto response = new OrderResponseDto();
        response.setId("test-id");
        when(service.getById("test-id")).thenReturn(response);

        mockMvc.perform(get("/orders/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id"));
    }

    @Test
    void getFiltered_Success() throws Exception {
        OrderResponseDto response = new OrderResponseDto();
        response.setId("test-id");
        when(service.getFiltered("NEW", "123")).thenReturn(List.of(response));

        mockMvc.perform(get("/orders").param("status", "NEW").param("customerId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("test-id"));
    }

    @Test
    void updateStatus_Success() throws Exception {
        OrderResponseDto response = new OrderResponseDto();
        response.setId("test-id");
        when(service.updateStatus("test-id", any())).thenReturn(response);

        mockMvc.perform(patch("/orders/test-id/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id"));
    }

    @Test
    void health_Success() throws Exception {
        mockMvc.perform(get("/orders/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK - Mongo, Redis, Kafka connected"));
    }
}