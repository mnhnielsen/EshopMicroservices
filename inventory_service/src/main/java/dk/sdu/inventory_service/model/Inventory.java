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
    private String productId;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String bikeTypeId;
    private String bikeType;

    public Inventory(String productId, String name, String description, double price, int stock, String bikeTypeId, String bikeType) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.bikeTypeId = bikeTypeId;
        this.bikeType = bikeType;
    }
}
