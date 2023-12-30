package dk.sdu.cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Payment {
    private String customerId;
    private String orderId;
    private String orderStatus;
}
