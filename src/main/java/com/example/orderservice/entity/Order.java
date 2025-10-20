package com.example.orderservice.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String id = UUID.randomUUID().toString();
    @Field("customerId")
    private String customerId;
    @Field("status")
    private String status = "NEW";  // Default
    @Field("items")
    private List<Item> items;
    @Field("createdAt")
    private LocalDateTime createdAt = LocalDateTime.now();
    @Field("updatedAt")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Data
    public static class Item {
        @Field("sku")
        private String sku;
        @Field("quantity")
        private int quantity;
        @Field("price")
        private double price;
    }
}