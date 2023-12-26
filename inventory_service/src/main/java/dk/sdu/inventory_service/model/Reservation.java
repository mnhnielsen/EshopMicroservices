package dk.sdu.inventory_service.model;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@AllArgsConstructor
@Data
@Builder
@Document(value = "reservation")
public class Reservation {
    private String customerId;
    @DBRef
    private String productId;
    private int quantity;
}
