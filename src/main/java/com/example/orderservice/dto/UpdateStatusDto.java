package com.example.orderservice.dto;

import lombok.Data;

@Data
public class UpdateStatusDto {
    private String status;  // NEW, IN_PROGRESS, DELIVERED, CANCELLED
}