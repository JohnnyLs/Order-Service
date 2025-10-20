package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderDto;
import com.example.orderservice.dto.OrderResponseDto;
import com.example.orderservice.dto.UpdateStatusDto;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService service;

    @PostMapping
    public ResponseEntity<OrderResponseDto> create(@RequestBody CreateOrderDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getFiltered(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId) {
        return ResponseEntity.ok(service.getFiltered(status, customerId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateStatus(@PathVariable String id, @RequestBody UpdateStatusDto dto) {
        return ResponseEntity.ok(service.updateStatus(id, dto));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        // Chequea conexiones básicas (usa actuator para más detalles)
        return ResponseEntity.ok("OK - Mongo, Redis, Kafka connected");
    }
}