package dk.sdu.inventory_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelOrderDto {
    private String orderId;
    private String customerId;
    private int quantity;
}
