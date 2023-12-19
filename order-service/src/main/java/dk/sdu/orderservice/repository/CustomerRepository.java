package dk.sdu.orderservice.repository;

import dk.sdu.orderservice.dto.CustomerDto;
import dk.sdu.orderservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
