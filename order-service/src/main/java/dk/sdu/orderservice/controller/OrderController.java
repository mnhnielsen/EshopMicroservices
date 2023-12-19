package dk.sdu.orderservice.controller;

import dk.sdu.orderservice.dto.CustomerDto;
import dk.sdu.orderservice.dto.OrderDto;
import dk.sdu.orderservice.dto.OrderProductDto;
import dk.sdu.orderservice.dto.PaymentDto;
import dk.sdu.orderservice.mapper.OrderDtoMapper;
import dk.sdu.orderservice.service.OrderService;
import lombok.extern.slf4j.Slf4j;
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
    private final String pubSubName = "kafka-pubsub";
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping(value = "/status")
    @ResponseStatus(HttpStatus.OK)
    public String getStatus() {
        return "Connected to order-service";
    }

    @GetMapping(value = "/{orderId}")
    public ResponseEntity<Optional<OrderDto>> getOrder(@PathVariable String orderId) {
        orderService.getOrder(orderId);
        System.out.println(orderService.getOrder(orderId));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Void> addOrder(@RequestBody OrderDto orderDto) {
        orderService.addOrder(orderDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/submit/{orderId}")
    public CompletableFuture<ResponseEntity<Object>> submitOrder(@PathVariable String orderId, @RequestBody CustomerDto customer) {
        return orderService.getOrder(orderId)
                .thenCompose(optionalOrderDto -> {
                    if (optionalOrderDto.isPresent()) {
                        OrderDto orderDto = optionalOrderDto.get();
                        orderDto.setOrderStatus("Reserved");
                        log.info(orderDto.getOrderStatus());

                        var customerToSave = new CustomerDto(customer.getName(), customer.getEmail(), customer.getAddress());
                        log.info(String.valueOf(customerToSave));
                        orderService.addCustomer(customerToSave);

                        return orderService.addOrder(orderDto)
                                .thenApply(finalOrderDto -> {
                                    var result = new PaymentDto(finalOrderDto.getCustomerId(), finalOrderDto.getOrderId(), finalOrderDto.getOrderStatus());
                                    log.info("ORDER SUBMITTED");
                                    orderService.publishEvent(pubSubName, "On_Order_Submit", result);
                                    log.info("Sending order to pubsub: {}", result);
                                    return ResponseEntity.ok().build();
                                });
                    } else {
                        return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
                    }
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
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        orderService.deleteOrder(orderId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}