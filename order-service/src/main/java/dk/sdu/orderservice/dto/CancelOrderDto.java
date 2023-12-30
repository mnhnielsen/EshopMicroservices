package dk.sdu.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelOrderDto {
    public String orderId;
    public String customerId;
    public int quantity;
}
