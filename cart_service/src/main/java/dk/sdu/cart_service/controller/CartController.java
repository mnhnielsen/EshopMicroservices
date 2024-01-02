package dk.sdu.cart_service.controller;

import dk.sdu.cart_service.model.PaymentDto;
import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.model.ReservationEvent;
import dk.sdu.cart_service.service.CartService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import io.dapr.client.domain.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
@RequestMapping("api/cart")
public class CartController {
    public final String pubSubName = "kafka-pubsub";
    public final String redisStateStore = "cart-store";
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);
    private final CartService cartService;

    @Autowired
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
    public ResponseEntity<Object> getReservation(@PathVariable String customerId) throws URISyntaxException, IOException, InterruptedException {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer ID cannot be null or empty");
        }
        try {
            DaprClient client = new DaprClientBuilder().build();
            State<Reservation> reservationState = client.getState(redisStateStore, customerId, Reservation.class).block();
            var res = reservationState.getValue();
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
            DaprClient client = new DaprClientBuilder().build();
            State<Reservation> reservationState = client.getState(redisStateStore, customerId, Reservation.class).block();
            var res = reservationState.getValue();
            if (res == null) {
                logger.info("No reservation found for: {}", customerId);
                return ResponseEntity.notFound().build();
            }

            client.saveState(redisStateStore, customerId, reservation).block();
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
            DaprClient client = new DaprClientBuilder().build();
            var res = cartService.getCartById(customerId);
            if (res == null) {
                logger.info("No reservation found for: {}", customerId);
                return ResponseEntity.notFound().build();
            }
            cartService.removeCart(res.getCustomerId());
            logger.info("Reservation deleted for: {}", customerId);
            for (var item : res.getItems()) {
                ReservationEvent reservationEvent = new ReservationEvent(res.getCustomerId(), item.getQuantity(), item.getProductId());
                client.publishEvent(pubSubName, "On_Products_Released", reservationEvent).block();
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
    public ResponseEntity<String> reserveProduct(@RequestBody(required = false) Reservation reservation) {
        try {
            DaprClient client = new DaprClientBuilder().build();
            if (reservation == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            //cartService.saveReservation(reservation);
            client.saveState(redisStateStore, reservation.getCustomerId(), reservation).block();
            for (var item : reservation.getItems()) {
                ReservationEvent reservationEvent = new ReservationEvent(reservation.getCustomerId(), item.getQuantity(), item.getProductId());
                client.publishEvent(pubSubName, "On_Products_Reserved", reservationEvent).block();
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
            DaprClient client = new DaprClientBuilder().build();
            State<Reservation> reservationState = client.getState(redisStateStore, customerId, Reservation.class).block();
            var res = reservationState.getValue();
            if (res == null) {
                logger.info("No reservation found for: {}", customerId);
                return ResponseEntity.notFound().build();
            }
            client.publishEvent(pubSubName, "On_Checkout", res).block();
            logger.info("Reservation completed for: {}", customerId);
            return ResponseEntity.ok().body(String.valueOf(res.getCustomerId()));
        } catch (Exception e) {
            logger.error("Error checking out reservation: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/orderSubmit")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Order_Submit", pubsubName = pubSubName)
    public Mono<ResponseEntity<String>> removeWhenOrderSubmit(@RequestBody(required = false) CloudEvent<PaymentDto> cloudEvent) {
        return Mono.fromSupplier(() -> {
            try {
                DaprClient client = new DaprClientBuilder().build();
                var paymentDto = cloudEvent.getData();
                var customerId = paymentDto.getOrderId();
                logger.info("CustomerID: " + customerId);
                client.deleteState(redisStateStore, customerId).block();
                return ResponseEntity.ok().body(String.valueOf(paymentDto.getCustomerId()));
            } catch (Exception e) {
                logger.error("Error deleting reservation: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private Reservation getStateStore(String stateStoreName, String key) throws Exception {
        try (DaprClient client = (new DaprClientBuilder()).build()) {
            // Get state
            State<Reservation> retrievedMessage = client.getState(stateStoreName, key, Reservation.class).block();

            if(retrievedMessage == null)
                return null;

            return retrievedMessage.getValue();
        }
    }

    @PostMapping(value = "/failedReservation")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Reservation_Failed", pubsubName = pubSubName)
    public Mono<ResponseEntity<?>> failedReservation(@RequestBody(required = false) CloudEvent<ReservationEvent> cloudEvent) {
        return Mono.fromSupplier(() ->{
            try {
                DaprClient client = new DaprClientBuilder().build();
                var reservationEvent = cloudEvent.getData();
                State<Reservation> reservationState = client.getState(redisStateStore, reservationEvent.getCustomerId(), Reservation.class).block();
                var res = reservationState.getValue();
                if (res == null) {
                    logger.info("No reservation found for: {}", reservationEvent.getCustomerId());
                    return ResponseEntity.notFound().build();
                }
                client.deleteState(redisStateStore, res.getCustomerId()).block();
                logger.info("Reservation deleted for: {}", reservationEvent.getCustomerId());
                return ResponseEntity.ok().body(String.valueOf(reservationEvent.getCustomerId()));
            } catch (Exception e) {
                logger.error("Error deleting reservation: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

}

