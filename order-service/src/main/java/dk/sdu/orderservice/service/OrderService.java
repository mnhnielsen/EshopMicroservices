package dk.sdu.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.orderservice.dto.CancelOrderDto;
import dk.sdu.orderservice.dto.OrderDto;
import dk.sdu.orderservice.dto.OrderProductDto;
import dk.sdu.orderservice.dto.PaymentDto;
import dk.sdu.orderservice.mapper.OrderDtoMapper;
import dk.sdu.orderservice.model.Order;
import dk.sdu.orderservice.model.OrderProduct;
import dk.sdu.orderservice.repository.OrderProductRepository;
import dk.sdu.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderProductRepository orderProductRepository;
    private final OrderDtoMapper orderDtoMapper;

    private final String DAPR_HOST = System.getenv().getOrDefault("DAPR_HOST", "http://localhost");
    private final String DAPR_HTTP_PORT = System.getenv().getOrDefault("DAPR_HTTP_PORT", "3500");
    private static HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();



    public Optional<OrderDto> getOrder(String id) {
        try {
            var order = orderRepository.findById(id);

            if (order.isPresent()) {
                return order.map(orderDtoMapper);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("Error retrieving order: {}", e.getMessage(), e);
            throw new RuntimeException("Error retrieving order", e);
        }
    }

    public void addOrder(OrderDto orderDto) {
        if (orderDto == null || orderDto.getOrderId() == null || orderDto.getCustomerId() == null || orderDto.getOrderProducts() == null) {
            log.error("Invalid OrderDto: {}", orderDto);
            throw new IllegalArgumentException("Invalid OrderDto: One or more required fields are null");
        }
        try {
            Order order = Order.builder()
                    .orderId(orderDto.getOrderId())
                    .customerId(orderDto.getCustomerId())
                    .orderStatus(orderDto.getOrderStatus())
                    .orderProducts(orderDto.getOrderProducts())
                    .build();

            orderRepository.save(order);
            log.info("Saved order {}", order.getOrderId());
        } catch (Exception e) {
            log.error("Error saving order: {}", e.getMessage(), e);
            throw new RuntimeException("Error saving order", e);
        }
    }

    public void addProductToOrder(String orderId, OrderProductDto orderProductDto) {
        try {
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
                log.error("Order does not exist for orderId: {}", orderId);
                throw new RuntimeException("Order does not exist");
            }
        } catch (Exception e) {
            log.error("Error adding product to order: {}", e.getMessage(), e);
            throw new RuntimeException("Error adding product to order", e);
        }
    }

    public void deleteOrder(String id) {
        try {
            Optional<Order> orderOptional = orderRepository.findById(id);
            if (orderOptional.isPresent()) {
                Order order = orderOptional.get();
                orderRepository.deleteById(id);
                log.info("Order {} has been removed", order.getOrderId());
            } else {
                log.error("Order with ID {} not found. Unable to delete.", id);
                throw new RuntimeException("Order not found");
            }
        } catch (Exception e) {
            log.error("Error deleting order with ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Error deleting order", e);
        }
    }

    public boolean orderExists(String orderId) {
        return orderRepository.existsById(orderId);
    }

    public <T> void publishEvent(String pubSubName, String topic, T payload) {
        try {
            String uri = DAPR_HOST + ":" + DAPR_HTTP_PORT + "/v1.0/publish/" + pubSubName + "/" + topic;
            ObjectMapper objectMapper = new ObjectMapper();
            String payloadJson = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .header("Content-Type", "application/json")
                    .build();

            System.out.println(request);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.OK.value()) {
                System.out.println(uri + " " + response.body());
            } else {
                System.err.println("Failed to publish event. Status code: " + response.statusCode());
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error converting payload to JSON: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error sending HTTP request: " + e.getMessage());
        }
    }


}
