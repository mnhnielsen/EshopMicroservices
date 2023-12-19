package dk.sdu.orderservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    public String customerId;
    public String name;
    public String email;
    public String address;
}
