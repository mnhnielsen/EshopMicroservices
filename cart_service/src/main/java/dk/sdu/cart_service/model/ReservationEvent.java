package dk.sdu.cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class ReservationEvent {
    private String customerId;
    private int quantity;
    private String productId;
}
