package dk.sdu.inventory_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ReservationEvent {
    private String customerId;
    private int quantity;
    private String productId;
}

