package dk.sdu.orderservice.model;

import jakarta.persistence.*;
import jdk.jfr.Timestamp;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
    public String orderStatus;
    @OneToMany @Fetch(FetchMode.JOIN)
    public List<OrderProduct> orderProducts;
}
