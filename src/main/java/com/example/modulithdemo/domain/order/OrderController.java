package com.example.modulithdemo.domain.order;

import java.util.UUID;
import java.util.logging.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

  private static final Logger logger = Logger.getLogger(OrderController.class.getName());

  private final OrderManagement orders;

  public OrderController(OrderManagement orders) {
    this.orders = orders;
  }

  @PostMapping("/{id}/complete")
  public ResponseEntity<String> complete(@PathVariable UUID id) {
    try {
      logger.info("Received request to complete order: " + id);

      orders.complete(id);

      String message = "Order " + id + " completed (event published)";
      logger.info("Successfully completed order: " + id);

      return ResponseEntity.accepted().body(message);

    } catch (OrderCompletionException e) {
      logger.severe("Failed to complete order " + id + ": " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to complete order " + id + ": " + e.getMessage());

    } catch (IllegalArgumentException e) {
      logger.warning("Invalid order ID provided: " + id + " - " + e.getMessage());
      return ResponseEntity.badRequest()
          .body("Invalid order ID: " + e.getMessage());

    } catch (Exception e) {
      logger.severe("Unexpected error completing order " + id + ": " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Unexpected error occurred while completing order " + id);
    }
  }
}