package dk.sdu.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.orderservice.dto.CustomerDto;
import dk.sdu.orderservice.dto.OrderDto;
import dk.sdu.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
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
public class OrderSubmitIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    public void testSubmitOrderSuccess() throws Exception {
        // Given
        String orderId = "998saqlkljh";
        CustomerDto customer = new CustomerDto("John Doe", "john@example.com", "123 Street");
        OrderDto orderDto = new OrderDto(); // fill this with appropriate data
        Mockito.when(orderService.getOrder(orderId)).thenReturn(CompletableFuture.completedFuture(Optional.of(orderDto)));
        Mockito.when(orderService.addOrder(any(OrderDto.class))).thenReturn(CompletableFuture.completedFuture(orderDto));

        // When & Then
        mockMvc.perform(post("/submit/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").doesNotExist());

        // Verify interactions
        Mockito.verify(orderService).addCustomer(any(CustomerDto.class));
        Mockito.verify(orderService).publishEvent(anyString(), anyString(), any());
    }

}
