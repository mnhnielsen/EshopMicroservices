package dk.sdu.orderservice.service;

import dk.sdu.orderservice.dto.OrderDto;
import dk.sdu.orderservice.dto.OrderProductDto;
import dk.sdu.orderservice.mapper.OrderDtoMapper;
import dk.sdu.orderservice.model.Order;
import dk.sdu.orderservice.model.OrderProduct;
import dk.sdu.orderservice.repository.OrderProductRepository;
import dk.sdu.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final OrderDtoMapper orderDtoMapper;

    private String getCustomerId() {
        return "abc123";
    }

    public Optional<OrderDto> getOrder(String id) {
        String currentUser = getCustomerId();
        var order = orderRepository.findById(id);
        if (order.isPresent() && order.get().getCustomerId().equals(currentUser)) {
            return order.map(orderDtoMapper);
        } else {
            return Optional.empty();
        }
    }

    public void addOrder(OrderDto orderDto) {
        Order order = Order.builder()
                .orderId(orderDto.getOrderId())
                .customerId(orderDto.getCustomerId())
                .orderProducts(orderDto.getOrderProducts())
                .build();
        orderRepository.save(order);
        log.info("Saved order {}", order.orderId);
    }

    public void addProductToOrder(String orderId, OrderProductDto orderProductDto) {
        if (orderExists(orderId)) {
            OrderProduct orderProduct = OrderProduct.builder()
                    .id(orderProductDto.getId())
                    .productId(orderProductDto.getProductId())
                    .orderId(orderProductDto.orderId = orderId)
                    .price(orderProductDto.getPrice())
                    .quantity(orderProductDto.getQuantity())
                    .build();
            orderProductRepository.save(orderProduct);
            log.info("Product added {}", orderProduct.getProductId());
        } else {
            log.info("Order does not exist");
            System.out.println("Order does not exist");
        }
    }

    public void deleteOrder(String id) {
        Order order = Order.builder().build();
        orderRepository.deleteById(id);
        log.info("Order {} has been removed", order.getOrderId());
    }

    public boolean orderExists(String orderId) {
        return orderRepository.existsById(orderId);
    }
}
