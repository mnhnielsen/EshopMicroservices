package dk.sdu.cart_service.repository;

import dk.sdu.cart_service.model.Reservation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisRepository extends CrudRepository<Reservation, String> {
}
