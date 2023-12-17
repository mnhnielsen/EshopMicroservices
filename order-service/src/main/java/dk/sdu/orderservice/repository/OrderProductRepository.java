package dk.sdu.orderservice.repository;

import dk.sdu.orderservice.model.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderProductRepository extends JpaRepository<OrderProduct, Integer> {

}
