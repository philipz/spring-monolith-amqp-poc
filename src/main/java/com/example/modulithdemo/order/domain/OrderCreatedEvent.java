package com.example.modulithdemo.order.domain;

public record OrderCreatedEvent(String orderNumber, String productCode, int quantity, Customer customer) {}
