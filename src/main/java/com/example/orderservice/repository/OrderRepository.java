package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByStatusAndCustomerId(String status, String customerId);

    @Query("{ 'status': ?0 }")
    List<Order> findByStatus(String status);
}