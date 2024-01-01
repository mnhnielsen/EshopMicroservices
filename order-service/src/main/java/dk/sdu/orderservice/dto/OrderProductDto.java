package dk.sdu.orderservice.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductDto {
    private int id;
    private String orderId;
    private String productId;
    private double price;
    private int quantity;

    public OrderProductDto(String orderId, String productId, double price, int quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.price = price;
        this.quantity = quantity;
    }
}
