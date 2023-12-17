package dk.sdu.orderservice.controller;

import dk.sdu.orderservice.dto.OrderDto;
import dk.sdu.orderservice.dto.OrderProductDto;
import dk.sdu.orderservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("api/order")
public class OrderController {

    private final OrderService orderService;

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
    public ResponseEntity<Optional<OrderDto>> getOrder(@PathVariable String orderId){
        orderService.getOrder(orderId);
        System.out.println(orderService.getOrder(orderId));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Void> addOrder(@RequestBody OrderDto orderDto) {
        orderService.addOrder(orderDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/add/{orderId}")
    public ResponseEntity<Void> addProductToOrder(@PathVariable String orderId ,@RequestBody OrderProductDto orderProductDto ) {
        orderService.addProductToOrder(orderId, orderProductDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping(value = "/delete/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        orderService.deleteOrder(orderId);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}