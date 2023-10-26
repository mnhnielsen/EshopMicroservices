package dk.sdu.inventory_service.repository;

import dk.sdu.inventory_service.model.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryRepository extends MongoRepository<Inventory, String> {
}
