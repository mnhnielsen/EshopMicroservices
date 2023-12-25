package dk.sdu.cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@RedisHash("Reservation")
@Builder
public class Reservation implements Serializable {
    @Id
    private String customerId;
    private List<Item> items;
}
