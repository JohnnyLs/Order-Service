package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderDto;
import com.example.orderservice.dto.OrderResponseDto;
import com.example.orderservice.dto.UpdateStatusDto;
import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService service;

    private CreateOrderDto createDto;
    private Order order;

    @BeforeEach
    void setUp() {
        createDto = new CreateOrderDto();
        createDto.setCustomerId("123");
        createDto.setItems(List.of(new com.example.orderservice.dto.ItemDto("SKU1", 1, 10.0)));

        order = new Order();
        order.setId("test-id");
        order.setCustomerId("123");
        order.setStatus("NEW");
        order.setItems(List.of(new com.example.orderservice.entity.Item("SKU1", 1, 10.0)));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void create_Success() {
        when(repository.save(any(Order.class))).thenReturn(order);

        OrderResponseDto result = service.create(createDto);

        assertNotNull(result.getId());
        assertEquals("123", result.getCustomerId());
        verify(repository).save(any(Order.class));
    }

    @Test
    void getById_Success() {
        when(repository.findById("test-id")).thenReturn(Optional.of(order));

        OrderResponseDto result = service.getById("test-id");

        assertEquals("test-id", result.getId());
        verify(repository).findById("test-id");
    }

    @Test
    void getFiltered_ByStatusAndCustomer_Success() {
        when(repository.findByStatusAndCustomerId("NEW", "123")).thenReturn(List.of(order));

        List<OrderResponseDto> result = service.getFiltered("NEW", "123");

        assertEquals(1, result.size());
        verify(repository).findByStatusAndCustomerId("NEW", "123");
    }

    @Test
    void updateStatus_PublishesEventAndUpdates() throws Exception {
        when(repository.findById("test-id")).thenReturn(Optional.of(order));
        when(repository.save(any(Order.class))).thenReturn(order);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"event\":\"data\"}");

        UpdateStatusDto dto = new UpdateStatusDto();
        dto.setStatus("DELIVERED");

        service.updateStatus("test-id", dto);

        verify(repository).save(any(Order.class));
        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        assertEquals("orders.events", captor.getValue().topic());
        verify(objectMapper).writeValueAsString(any());
    }

    @Test
    void updateStatus_InvalidStatus_ThrowsException() {
        when(repository.findById("test-id")).thenReturn(Optional.of(order));

        UpdateStatusDto dto = new UpdateStatusDto();
        dto.setStatus("INVALID");

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus("test-id", dto));
    }
}