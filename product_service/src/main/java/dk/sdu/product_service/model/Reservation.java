package dk.sdu.product_service.model;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Reservation {
    public String customerId;
    public int quantity;
    public String productId;
}
