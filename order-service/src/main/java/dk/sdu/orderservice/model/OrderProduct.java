package dk.sdu.orderservice.model;


import jakarta.persistence.*;
import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "OrderProduct")
public class OrderProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;
    public String orderId;
    public String productId;
    public double price;
    public int quantity;


}
