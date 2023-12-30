package dk.sdu.cart_service.controller;

import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.model.ReservationEvent;
import dk.sdu.cart_service.service.CartService;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

@RestController
@RequestMapping("api/cart")
public class CartController {
    public final String pubSubName = "kafka-pubsub";
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private final CartService cartService;
    private static final String DAPR_SIDE_CAR_PORT = "3500"; // Replace with your Dapr sidecar port
    private static final String DAPR_PUBSUB_TOPIC = "On_Order_Submit"; // Replace with your Dapr pub/sub topic name



    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // Triggered by Dapr when a message is published to the specified topic
    @PostMapping("/dapr/subscribe")
    public ResponseEntity<String> daprSubscribe(@RequestBody String payload) {
        // Use RestTemplate to send a request to your own controller endpoint
        String url = "http://localhost:" + DAPR_SIDE_CAR_PORT + "/consumeMessages";
        ResponseEntity<String> response = new RestTemplate().postForEntity(url, payload, String.class);

        // Log or handle the response as needed
        System.out.println("Response from consumeMessages endpoint: " + response.getBody());

        return ResponseEntity.ok("Subscribe request processed successfully");
    }

    // Example method to publish a message to Dapr pub/sub
    @PostMapping("/publishMessage")
    public ResponseEntity<String> publishMessage(@RequestBody String message) {
        String url = "http://localhost:" + DAPR_SIDE_CAR_PORT + "/v1.0/publish/" + pubSubName + "/" + DAPR_PUBSUB_TOPIC;
        ResponseEntity<String> response = new RestTemplate().postForEntity(url, message, String.class);

        // Log or handle the response as needed
        System.out.println("Response from publishMessage endpoint: " + response.getBody());

        return ResponseEntity.ok("Message published successfully");
    }


    @GetMapping(value = "/status")
    @ResponseStatus(HttpStatus.OK)
    public String getStatus() {
        return "Connected to shopping cart";
    }

    @GetMapping("/{customerId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Object> getReservation(@PathVariable String customerId) throws URISyntaxException, IOException, InterruptedException {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer ID cannot be null or empty");
        }
        try {
            Reservation res = cartService.getCartById(customerId);
            if (res == null) {
                logger.info("No reservation found for: {}", customerId);
                return ResponseEntity.notFound().build();
            }
            logger.info(res.getCustomerId());
            return ResponseEntity.ok().body(res);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Customer ID", e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error accessing data", e);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Reservation addReservation(@RequestBody Reservation reservation) {
        cartService.saveReservation(reservation);
        var res = cartService.getCartById(reservation.getCustomerId());
        logger.info("Reservation created for: {}", reservation.getCustomerId());
        return res;
    }

    @PutMapping(value = "/{customerId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> updateCart(@PathVariable String customerId, @RequestBody Reservation reservation) {
        try {
            var res = cartService.getCartById(customerId);
            if (res == null) {
                logger.info("No reservation found for: {}", customerId);
                return ResponseEntity.notFound().build();
            }
            cartService.saveReservation(reservation);
            logger.info("Reservation updated for: {}", customerId);
            return ResponseEntity.ok().body(String.valueOf(res.getCustomerId()));
        } catch (Exception e) {
            logger.error("Error updating reservation: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping(value = "/{customerId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> removeReservation(@PathVariable String customerId) {
        try {
            var res = cartService.getCartById(customerId);
            if (res == null) {
                logger.info("No reservation found for: {}", customerId);
                return ResponseEntity.notFound().build();
            }
            cartService.removeCart(res.getCustomerId());
            logger.info("Reservation deleted for: {}", customerId);
            for (var item : res.getItems()) {
                ReservationEvent reservationEvent = new ReservationEvent(res.getCustomerId(), item.getQuantity(), item.getProductId());
                cartService.publishEvent(pubSubName, "On_Products_Released", reservationEvent);
                logger.info("product removed: " + item.getProductId());
            }
            return ResponseEntity.ok().body(String.valueOf(customerId));
        } catch (Exception e) {
            logger.error("Error deleting reservation: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/reserve")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> reserveProduct(@RequestBody(required = false) Reservation reservation) throws InterruptedException {
        DaprClient client = new DaprClientBuilder().build();
        try {
            if (reservation == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            cartService.saveReservation(reservation);
            for (var item : reservation.getItems()) {
                ReservationEvent reservationEvent = new ReservationEvent(reservation.getCustomerId(), item.getQuantity(), item.getProductId());
                client.publishEvent(
                        pubSubName,
                        "On_Products_Reserved",
                        reservationEvent).block();
                //cartService.publishEvent(pubSubName, "On_Products_Reserved", reservationEvent);
                logger.info("product added: " + item.getProductId());
            }
            logger.info("item added for user: " + reservation.getCustomerId());
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/checkout/{customerId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> checkout(@PathVariable String customerId) {
        try {
            var res = cartService.getCartById(customerId);
            if (res == null) {
                logger.info("No reservation found for: {}", customerId);
                return ResponseEntity.notFound().build();
            }
            cartService.publishEvent(pubSubName, "On_Reservation_Completed", res);
            logger.info("Reservation completed for: {}", customerId);
            return ResponseEntity.ok().body(String.valueOf(res.getCustomerId()));
        } catch (Exception e) {
            logger.error("Error checking out reservation: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

