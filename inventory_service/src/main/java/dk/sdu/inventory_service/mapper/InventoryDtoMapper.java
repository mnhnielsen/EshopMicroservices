package dk.sdu.inventory_service.mapper;

import dk.sdu.inventory_service.dto.InventoryDto;
import dk.sdu.inventory_service.model.Inventory;
import org.springframework.stereotype.Service;

import java.util.function.Function;
@Service
public class InventoryDtoMapper implements Function<Inventory, InventoryDto> {

    @Override
    public InventoryDto apply(Inventory inventory) {
        return new InventoryDto(inventory.getId(),
                inventory.getName(),
                inventory.getDescription(),
                inventory.getPrice(),
                inventory.getStock(),
                inventory.getBikeType());
    }
}
