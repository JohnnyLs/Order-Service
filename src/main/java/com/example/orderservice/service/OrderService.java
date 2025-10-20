package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderDto;
import com.example.orderservice.dto.EventDto;
import com.example.orderservice.dto.OrderResponseDto;
import com.example.orderservice.dto.UpdateStatusDto;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MongoTemplate mongoTemplate;
    private static final String TOPIC = "orders.events";

    @Transactional
    public OrderResponseDto create(CreateOrderDto dto) {
        Order order = new Order();
        order.setCustomerId(dto.getCustomerId());
        order.setItems(dto.getItems().stream().map(this::mapToItem).collect(Collectors.toList()));
        Order saved = repository.save(order);
        log.info("Created order: {}", saved.getId());
        return mapToDto(saved);
    }

    @Cacheable(value = "orders", key = "#id")
    public OrderResponseDto getById(String id) {
        Order order = repository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        return mapToDto(order);
    }

    public List<OrderResponseDto> getFiltered(String status, String customerId) {
        List<Order> orders;
        if (status != null && customerId != null) {
            orders = repository.findByStatusAndCustomerId(status, customerId);
        } else if (status != null) {
            orders = repository.findByStatus(status);
        } else {
            orders = repository.findAll();
        }
        return orders.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @CacheEvict(value = "orders", key = "#id")
    @Transactional
    public OrderResponseDto updateStatus(String id, UpdateStatusDto dto) {
        Order order = repository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
        String oldStatus = order.getStatus();
        String newStatus = dto.getStatus();
        if (!List.of("NEW", "IN_PROGRESS", "DELIVERED", "CANCELLED").contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status");
        }

        // Update in Mongo
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        repository.save(order);

        // Publish event
        EventDto event = new EventDto();
        event.setOrderId(id);
        event.setOldStatus(oldStatus);
        event.setNewStatus(newStatus);
        event.setTimestamp(LocalDateTime.now());
        try {
            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(new ProducerRecord<>(TOPIC, id, message));
            log.info("Published event for order {}: {} -> {}", id, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("Error publishing event", e);
            throw new RuntimeException("Failed to publish event", e);
        }

        return mapToDto(order);
    }

    private Order.Item mapToItem(com.example.orderservice.dto.ItemDto dtoItem) {
        Order.Item item = new Order.Item();
        item.setSku(dtoItem.getSku());
        item.setQuantity(dtoItem.getQuantity());
        item.setPrice(dtoItem.getPrice());
        return item;
    }

    private OrderResponseDto mapToDto(Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.setId(order.getId());
        dto.setCustomerId(order.getCustomerId());
        dto.setStatus(order.getStatus());
        dto.setItems(order.getItems().stream().map(this::mapToItemDto).collect(Collectors.toList()));
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }

    private com.example.orderservice.dto.ItemDto mapToItemDto(Order.Item item) {
        com.example.orderservice.dto.ItemDto dto = new com.example.orderservice.dto.ItemDto();
        dto.setSku(item.getSku());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        return dto;
    }
}