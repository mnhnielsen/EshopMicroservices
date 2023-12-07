package dk.sdu.cart_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.model.ReservationEvent;
import dk.sdu.cart_service.service.CartService;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


@RestController
@RequestMapping("api/cart")
public class CartController {
    public final String redisStore = "cart-store";
    public final String pubSubName = "kafka-commonpubsub";
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private final CartService cartService;
    private final String DAPR_HOST = System.getenv().getOrDefault("DAPR_HOST", "http://localhost");
    private final String DAPR_HTTP_PORT = System.getenv().getOrDefault("DAPR_HTTP_PORT", "3500");
    private static HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping(value = "/status")
    @ResponseStatus(HttpStatus.OK)
    public String getStatus() {
        return "Connected to shopping cart";
    }

    @GetMapping("/{customerId}")
    @ResponseStatus(HttpStatus.OK)
    public String getReservation(Reservation reservation, @PathVariable String customerId) throws URISyntaxException, IOException, InterruptedException {
        customerId = reservation.getCustomerId();
        URI baseUrl = new URI(DAPR_HOST+":"+DAPR_HTTP_PORT);
        URI getStateURL = new URI(baseUrl + "/v1.0/state/"+redisStore+"/"+ customerId);
        logger.info(baseUrl + "/v1.0/state/"+redisStore+"/"+ customerId);
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(getStateURL)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Order saved: "+ response.body());
        return response.body();
    }

    @PostMapping(value = "/reserve")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> reserveProduct(@RequestBody(required = false) Reservation reservation) {
        try {
            if (reservation == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            cartService.saveState(redisStore, reservationToJsonBytes(reservation));
            for (var item : reservation.getItems()) {
                cartService.publishEvent(pubSubName, "On_Products_Reserved", reservationEventToJsonBytes(reservation.getCustomerId(), item.getQuantity(), item.getProductId()));
                logger.info("product added: " + item.getProductId());
                logger.info(String.valueOf(reservation.getItems()));
            }
            logger.info("state added for user:" + reservation.getCustomerId());
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping(value = "/removeProduct/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> removeProduct(@PathVariable String id) {

        try(DaprClient daprClient = new DaprClientBuilder().build()) {
            var result = daprClient.getState(redisStore,id,Reservation.class).block();
            if (result == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            daprClient.deleteState(redisStore,id).block();
            logger.info("Deleting product: " + id);

            for (var item : result.getValue().items ) {
                var reservationEvent = new ReservationEvent(result.getValue().customerId, item.getQuantity(), item.getProductId());
                daprClient.publishEvent(pubSubName, "On_Products_Removed_Cart",reservationEvent).block();
            }
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] reservationToJsonBytes(Reservation reservation) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            logger.info(reservation.getCustomerId());
            return objectMapper.writeValueAsBytes(reservation);
        } catch (Exception e) {
            throw new RuntimeException("Error converting Reservation to JSON", e);
        }
    }

    private static byte[] reservationEventToJsonBytes(String customerId, int quantity, String productId) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ReservationEvent reservationEvent = new ReservationEvent(customerId, quantity, productId);
            logger.info(reservationEvent.getCustomerId() +' '+  reservationEvent.getProductId());
            return objectMapper.writeValueAsBytes(reservationEvent);
        } catch (Exception e) {
            throw new RuntimeException("Error converting ReservationEvent to JSON", e);
        }
    }
}


