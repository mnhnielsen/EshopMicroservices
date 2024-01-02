package dk.sdu.cart_service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@RedisHash("Reservation")
@Builder
@NoArgsConstructor
public class Reservation implements Serializable {
    @Id
    private String customerId;
    private List<Item> items;
}
