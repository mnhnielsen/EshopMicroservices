package dk.sdu.inventory_service.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(value = "inventory")
@Builder
@Data
@Getter
@Setter

public class Inventory {
    @Id
    private String id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String bikeTypeId;
    private String bikeType;

    public Inventory(String id, String name, String description, double price, int stock, String bikeTypeId, String bikeType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.bikeTypeId = bikeTypeId;
        this.bikeType = bikeType;
    }
}
