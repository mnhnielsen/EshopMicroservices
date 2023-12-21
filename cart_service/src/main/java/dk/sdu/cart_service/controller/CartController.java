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
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URISyntaxException;

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
    public void getReservation(@PathVariable String customerId) throws URISyntaxException, IOException, InterruptedException {
        cartService.getCart(customerId);
    }

    @PostMapping(value = "/reserve")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> reserveProduct(@RequestBody(required = false) Reservation reservation, String id) {
        try {
            if (reservation == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            cartService.saveReservation(id,reservation);
            for (var item : reservation.getItems()) {
                ReservationEvent reservationEvent = new ReservationEvent(reservation.getCustomerId(), item.getQuantity(), item.getProductId());
                cartService.publishEvent(pubSubName, "On_Products_Reserved", reservationEvent);
                logger.info("product added: " + item.getProductId());
            }
            logger.info("state added for user: " + reservation.getCustomerId());
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

//    @DeleteMapping(value = "/removeProduct/{id}")
//    @ResponseStatus(HttpStatus.OK)
//    public ResponseEntity<String> removeProduct(@PathVariable String id) {
//
//        try(DaprClient daprClient = new DaprClientBuilder().build()) {
//            var result = daprClient.getState(redisStore,id,Reservation.class).block();
//            if (result == null) {
//                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//            }
//            daprClient.deleteState(redisStore,id).block();
//            logger.info("Deleting product: " + id);
//
//            for (var item : result.getValue().items ) {
//                var reservationEvent = new ReservationEvent(result.getValue().customerId, item.getQuantity(), item.getProductId());
//                daprClient.publishEvent(pubSubName, "On_Products_Removed_Cart",reservationEvent).block();
//            }
//            return new ResponseEntity<>(HttpStatus.OK);
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
}


