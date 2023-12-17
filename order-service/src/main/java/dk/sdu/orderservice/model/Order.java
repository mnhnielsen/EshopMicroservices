package dk.sdu.orderservice.model;

import jakarta.persistence.*;
import jdk.jfr.Timestamp;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "Orders")
public class Order {
    @Id
    public String orderId;
    public String customerId;
    public String status;
    @OneToMany(cascade = CascadeType.ALL)
    public List<OrderProduct> orderProducts;
}
