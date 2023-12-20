package dk.sdu.orderservice;

import dk.sdu.orderservice.controller.OrderController;
import dk.sdu.orderservice.dto.CustomerDto;
import dk.sdu.orderservice.dto.OrderDto;
import dk.sdu.orderservice.dto.OrderProductDto;
import dk.sdu.orderservice.dto.PaymentDto;
import dk.sdu.orderservice.mapper.OrderDtoMapper;
import dk.sdu.orderservice.model.Customer;
import dk.sdu.orderservice.model.Order;
import dk.sdu.orderservice.model.OrderProduct;
import dk.sdu.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderDtoMapper orderDtoMapper;

    @InjectMocks
    private OrderController orderController;

    @Test
    void submitOrderTest() {
        CustomerDto customerDto = new CustomerDto("John","JohnDoe@email.com","JohnDoeAddress");

        OrderProduct orderProduct = new OrderProduct(1, "1", "1",20.5,5);
        List<OrderProduct> productList = new ArrayList<OrderProduct>();
        productList.add(orderProduct);
        OrderDto orderDto = new OrderDto("1","1","Test", productList);
        PaymentDto paymentDto = new PaymentDto();

        // Mocking orderService behavior
        when(orderService.getOrder(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(orderDto)));
        doNothing().when(orderService).addCustomer(customerDto);
        when(orderService.addOrder(any())).thenReturn(CompletableFuture.completedFuture(orderDto));

        // Perform the method call
        CompletableFuture<ResponseEntity<Object>> result = orderController.submitOrder(paymentDto.getOrderId(), customerDto);

        // Assert the result
        assertDoesNotThrow(() -> {
            ResponseEntity<Object> responseEntity = result.get();
            assertEquals(200, responseEntity.getStatusCode().value());
        });
    }
}

