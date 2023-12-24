package dk.sdu.cart_service.controller;

import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.model.ReservationEvent;
import dk.sdu.cart_service.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
        try {
            var res = cartService.getCartById(customerId);
            if (res == null) {
                return ResponseEntity.ok().body(res);
            } else {
                logger.info("No reservation found for customer: " + customerId);
                return ResponseEntity.notFound().build();
            }
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
        return cartService.getCartById(reservation.getCustomerId());
    }

    @PostMapping(value = "/reserve")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> reserveProduct(@RequestBody(required = false) Reservation reservation) {
        try {
            if (reservation == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            cartService.saveReservation(reservation);
            for (var item : reservation.getItems()) {
                ReservationEvent reservationEvent = new ReservationEvent(reservation.getCustomerId(), item.getQuantity(), item.getProductId());
                cartService.publishEvent(pubSubName, "On_Products_Reserved", reservationEvent);
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


