package dk.sdu.inventory_service.repository;

import dk.sdu.inventory_service.model.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
}
