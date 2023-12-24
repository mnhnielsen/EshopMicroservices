package dk.sdu.orderservice.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Data
@Table(name = "Customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;
    public String customerId;
    public String name;
    public String email;
    public String address;

    public Customer(String customerId, String name, String email, String address) {
    }
}
