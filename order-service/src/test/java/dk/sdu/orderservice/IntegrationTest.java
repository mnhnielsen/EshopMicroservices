package dk.sdu.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.orderservice.dto.CustomerDto;
import dk.sdu.orderservice.dto.OrderDto;
import dk.sdu.orderservice.model.OrderProduct;
import dk.sdu.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class IntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    public void testSubmitOrderSuccess() throws Exception {
        // Given
        OrderProduct orderProduct = new OrderProduct(1, "1", "1",20.5,5);
        List<OrderProduct> productList = new ArrayList<OrderProduct>();
        productList.add(orderProduct);


        CustomerDto customer = new CustomerDto("John Doe", "john@example.com", "123 Street");
        OrderDto orderDto = new OrderDto(); // fill this with appropriate data
        orderDto.setOrderId("1");
        orderDto = new OrderDto(orderDto.getOrderId(), "1","Pending", productList);
        Mockito.when(orderService.getOrder(orderDto.getOrderId())).thenReturn(CompletableFuture.completedFuture(Optional.of(orderDto)));
        Mockito.when(orderService.addOrder(any(OrderDto.class))).thenReturn(CompletableFuture.completedFuture(orderDto));
        String url = "/api/order/submit/" + orderDto.getOrderId();
        System.out.println(url);

        // When and Then
        mockMvc.perform(post(url, orderDto.getOrderId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());

        // Verify interactions
        Mockito.verify(orderService).addCustomer(any(CustomerDto.class));
        Mockito.verify(orderService).publishEvent(anyString(), anyString(), any());
    }

}
