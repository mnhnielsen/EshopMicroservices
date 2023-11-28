package dk.sdu.cart_service.controller;

import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.model.ReservationEvent;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;


@RestController
@RequestMapping("api/cart")
public class CartController {
    public final String redisStore = "cart-store";
    public final String pubSubName = "kafka-commonpubsub";
    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @GetMapping(value = "/status")
    @ResponseStatus(HttpStatus.OK)
    public String getStatus() {
        return "Connected to shopping cart";
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Optional<Reservation> getBasket(@PathVariable String id) {
        try (DaprClient daprClient = new DaprClientBuilder().build()) {
            var result = daprClient.getState(redisStore,id,Reservation.class);
            if (result == null) {
                throw new IllegalArgumentException("no items in basket");
            }
            return Optional.ofNullable(Objects.requireNonNull(result.block()).getValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(value = "/reserve")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> addProductToBasket(@RequestBody(required = false) Reservation reservation)
    {
        try(DaprClient daprClient = new DaprClientBuilder().build()){

            if (reservation == null || reservation.getItems().isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            daprClient.saveState(redisStore,reservation.getCustomerId(), Reservation.class).block();
            for (var item: reservation.getItems()) {
                var reservationEvent = new ReservationEvent(reservation.getCustomerId(), item.getQuantity(), item.getProductId());
                logger.info(item.getProductId() + " Has been saved");
                daprClient.publishEvent(pubSubName,"On_Products_Reserved",reservationEvent).block();
            }
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
}


