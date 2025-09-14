package com.example.modulithdemo.domain.order;

import org.springframework.modulith.events.Externalized;

@Externalized("BookStoreExchange::orders.new")
public record OrderCreatedEvent(String orderNumber, String productCode, int quantity, Customer customer) {}
