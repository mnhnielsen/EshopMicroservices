package dk.sdu.orderservice.mapper;

import dk.sdu.orderservice.dto.CustomerDto;
import dk.sdu.orderservice.dto.OrderDto;
import dk.sdu.orderservice.model.Customer;
import dk.sdu.orderservice.model.Order;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class OrderDtoMapper implements EntityMapper<Order, OrderDto> {
    @Override
    public OrderDto map(Order order) {
        return new OrderDto(order.getOrderId(),
                order.getCustomerId(),
                order.getOrderStatus(),
                order.getOrderProducts());
    }
}
