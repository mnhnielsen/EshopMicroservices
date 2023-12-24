package dk.sdu.orderservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jdk.jfr.Timestamp;
import lombok.*;
import org.hibernate.FetchMode;
import org.hibernate.annotations.Fetch;

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
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "orderId")
    public List<OrderProduct> orderProducts;
}
