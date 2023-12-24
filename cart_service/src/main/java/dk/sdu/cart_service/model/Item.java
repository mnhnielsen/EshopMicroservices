package dk.sdu.cart_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
public class Item implements Serializable {
    public int quantity;
    public double price;
    public String productId;
}
