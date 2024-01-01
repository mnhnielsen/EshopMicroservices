package dk.sdu.cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PaymentDto {
    private String customerId;
    private String orderId;
    private String orderStatus;
}
