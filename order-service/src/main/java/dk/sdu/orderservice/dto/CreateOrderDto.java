package dk.sdu.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CreateOrderDto {
    private String customerId;
    private List<OrderProductDto> orderProducts;
}
