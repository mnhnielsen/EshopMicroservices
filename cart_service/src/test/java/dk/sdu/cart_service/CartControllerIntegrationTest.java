package dk.sdu.cart_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.service.CartService;
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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Test
    public void testReserveProductSuccess() throws Exception {
        // Given
        Reservation reservation = new Reservation("customerId", Collections.emptyList());
        Mockito.doNothing().when(cartService).saveReservation(any(Reservation.class));

        // When and Then
        mockMvc.perform(post("/api/cart/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(reservation)))
                .andExpect(status().isCreated());
    }

    @Test
    public void testCheckoutSuccess() throws Exception {
        // Given
        String customerId = "customerId";
        Reservation reservation = new Reservation(customerId, Collections.emptyList());
        Mockito.when(cartService.getCartById(customerId)).thenReturn(reservation);
        Mockito.doNothing().when(cartService).publishEvent(anyString(), anyString(), any());

        // When and Then
        mockMvc.perform(post("/api/cart/checkout/{customerId}", customerId))
                .andExpect(status().isOk());
    }
}