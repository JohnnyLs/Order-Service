package com.example.orderservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateOrderDto {
    private String customerId;
    private List<ItemDto> items;
}

