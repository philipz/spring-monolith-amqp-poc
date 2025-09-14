package com.example.modulithdemo.order.domain;

public class OrderCompletionException extends RuntimeException {

  public OrderCompletionException(String message) {
    super(message);
  }

  public OrderCompletionException(String message, Throwable cause) {
    super(message, cause);
  }
}
