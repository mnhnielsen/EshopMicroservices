package dk.sdu.orderservice.controller;

import dk.sdu.orderservice.dto.*;
import dk.sdu.orderservice.mapper.OrderDtoMapper;
import dk.sdu.orderservice.model.Customer;
import dk.sdu.orderservice.model.Order;
import dk.sdu.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequestMapping("api/order")
public class OrderController {
    private final OrderService orderService;
    private final OrderDtoMapper orderDtoMapper;
    private final String pubSubName = "kafka-pubsub";
    @Autowired
    public OrderController(OrderService orderService, OrderDtoMapper orderDtoMapper) {
        this.orderService = orderService;
        this.orderDtoMapper = orderDtoMapper;
    }

    @GetMapping(value = "/status")
    @ResponseStatus(HttpStatus.OK)
    public String getStatus() {
        return "Connected to order-service";
    }

    @GetMapping(value = "/{orderId}")
    public CompletableFuture<Optional<OrderDto>> getOrder(@PathVariable String orderId) {
        var res = orderService.getOrder(orderId);
        System.out.println(res);
        Hibernate.initialize(res);
        return ResponseEntity.ok().body(res).getBody();
    }

    @PostMapping
    public ResponseEntity<Void> addOrder(@RequestBody OrderDto orderDto) {
        orderService.addOrder(orderDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("submit/{id}")
    public CompletableFuture<ResponseEntity<Object>> submitOrder(@PathVariable String id, @RequestBody CustomerDto customer) {
        return orderService.getOrder(id)
                .thenApply(order -> {
                    if (order.isEmpty()) {
                        throw new IllegalStateException("Order not found");
                    }
                    var finalOrder = new Order();
                    orderDtoMapper.map(finalOrder); // Map from DTO to entity
                    finalOrder.setOrderStatus("Reserved");
                    log.info("Order {} {} {}", finalOrder.getOrderId(), finalOrder.getCustomerId() ,finalOrder.getOrderStatus());

                    var customerToSave = new Customer(finalOrder.getCustomerId(), customer.getName(), customer.getEmail(), customer.getAddress());
                    log.info("Customer: {}", customerToSave);
                    if (finalOrder.getOrderId() == null || customerToSave.getName() == null || customerToSave.getEmail() == null || customerToSave.getAddress() == null) {
                        log.error("Invalid Customer: {}", customerToSave);
                        throw new IllegalArgumentException("Invalid CustomerDto: One or more required fields are null");
                    }
                    orderService.addCustomer(customerToSave);
                    return finalOrder;
                })
                .thenApply(saved -> {
                    var result = new PaymentDto(saved.getCustomerId(), saved.getOrderId(), saved.getOrderStatus());
                    orderService.publishEvent(pubSubName, "On_Order_Submit", result);
                    log.info("ORDER SUBMITTED");

                    return ResponseEntity.ok().build();
                })
                .exceptionally(ex -> {
                    log.error("Error processing order submission: {}", ex.getMessage(), ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }


    @PostMapping("/add/{orderId}")
    public ResponseEntity<Void> addProductToOrder(@PathVariable String orderId, @RequestBody OrderProductDto orderProductDto) {
        orderService.addProductToOrder(orderId, orderProductDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping(value = "/delete/{orderId}")
    public CompletableFuture<ResponseEntity<Object>> deleteOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
                .thenCompose(orderToDeleteDto -> {
                    if (orderToDeleteDto.isEmpty()) {
                        log.info("Order with ID {} not found. Unable to delete.", orderId);
                        return CompletableFuture.completedFuture(new ResponseEntity<>(HttpStatus.NOT_FOUND));
                    }
                    Order order = new Order();
                    orderToDeleteDto = Optional.ofNullable(orderDtoMapper.map(order));
                    orderService.deleteOrder(orderId);
                    for (var item : orderToDeleteDto.get().getOrderProducts()) {
                        var cancelOrder = new CancelOrderDto(orderId, orderToDeleteDto.get().getCustomerId(), item.getQuantity());
                        orderService.publishEvent(pubSubName, "On_Order_Cancel", cancelOrder);
                    }
                    log.info("Order with ID {} deleted successfully.", orderId);
                    return CompletableFuture.completedFuture(new ResponseEntity<>(HttpStatus.OK));
                })
                .exceptionally(ex -> {
                    log.error("Error while deleting order with ID {}: {}", orderId, ex.getMessage(), ex);
                    return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }
}