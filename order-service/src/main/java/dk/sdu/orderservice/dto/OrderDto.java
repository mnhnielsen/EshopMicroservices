package dk.sdu.orderservice.dto;

import dk.sdu.orderservice.model.OrderProduct;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    private String orderId;
    private String customerId;
    private String orderStatus;
    private List<OrderProduct> orderProducts;
}
