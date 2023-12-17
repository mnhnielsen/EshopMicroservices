package dk.sdu.orderservice.mapper;

import dk.sdu.orderservice.dto.OrderDto;
import dk.sdu.orderservice.model.Order;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class OrderDtoMapper implements Function<Order, OrderDto> {
    @Override
    public OrderDto apply(Order order) {
        return new OrderDto(order.getOrderId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getOrderProducts());
    }
}
