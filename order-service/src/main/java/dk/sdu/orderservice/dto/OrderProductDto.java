package dk.sdu.orderservice.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductDto {
    public int id;
    public String orderId;
    public String productId;
    public double price;
    public int quantity;
}
