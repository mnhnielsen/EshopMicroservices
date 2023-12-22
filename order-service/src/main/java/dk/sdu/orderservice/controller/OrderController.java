package dk.sdu.orderservice.controller;

import dk.sdu.orderservice.dto.*;
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
        return ResponseEntity.ok().body(res).getBody();
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
                        log.info("Order not found :{} ", orderId);
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
        try {
            if (!orderService.orderExists(orderId)) {
                log.info("Order with ID {} not found. Unable to delete.", orderId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            var orderToDelete = orderService.getOrder(orderId);
            if (orderToDelete == null) {
                log.info("Order with ID {} not found. Unable to delete.", orderId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            var res = orderDtoMapper.map(null);
            //orderToDelete.get().get()
            orderService.deleteOrder(orderId);
            for (var item : res.getOrderProducts()) {
                var cancelOrder = new CancelOrderDto(orderId, res.getCustomerId(), item.getQuantity());
                orderService.publishEvent(pubSubName, "On_Order_Cancel", cancelOrder);
            }
            log.info("Order with ID {} deleted successfully.", orderId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error while deleting order with ID {}: {}", orderId, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}