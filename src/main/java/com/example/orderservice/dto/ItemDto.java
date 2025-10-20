package com.example.orderservice.dto;

import lombok.Data;

@Data
public class ItemDto {
    private String sku;
    private int quantity;
    private double price;
}