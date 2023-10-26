package dk.sdu.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ProductDto {
    private String id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String bikeType;

}
