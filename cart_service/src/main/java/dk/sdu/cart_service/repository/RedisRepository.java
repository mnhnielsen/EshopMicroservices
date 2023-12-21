package dk.sdu.cart_service.repository;

import dk.sdu.cart_service.model.Reservation;
import org.springframework.data.repository.CrudRepository;

public interface RedisRepository extends CrudRepository<Reservation, String> {
}
