package dk.sdu.orderservice.dto;

import dk.sdu.orderservice.model.OrderProduct;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    public String orderId;
    public String customerId;
    public String orderStatus;
    public List<OrderProduct> orderProducts;
}
