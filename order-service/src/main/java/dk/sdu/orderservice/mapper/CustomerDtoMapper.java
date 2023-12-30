package dk.sdu.orderservice.mapper;

import dk.sdu.orderservice.dto.CustomerDto;
import dk.sdu.orderservice.model.Customer;
import org.springframework.stereotype.Service;

@Service
public class CustomerDtoMapper implements EntityMapper<Customer, CustomerDto>{
    @Override
    public CustomerDto map(Customer customer) {
        return new CustomerDto(customer.getEmail(),
                customer.getName(),
                customer.getAddress());
    }
}
