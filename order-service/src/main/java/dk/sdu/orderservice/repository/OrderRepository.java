package dk.sdu.orderservice.repository;

import dk.sdu.orderservice.model.Order;
import dk.sdu.orderservice.model.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
}

