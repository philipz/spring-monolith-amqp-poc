package com.example.modulithdemo.messaging.inbound.amqp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewOrderMessage(
  String orderNumber,
  String productCode,
  int quantity,
  NewOrderCustomer customer
) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static record NewOrderCustomer(
    String name,
    String email,
    String phone
  ) {}
}

