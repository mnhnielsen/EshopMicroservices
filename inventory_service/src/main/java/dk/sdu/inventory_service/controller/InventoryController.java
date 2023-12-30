package dk.sdu.inventory_service.controller;

import dk.sdu.inventory_service.dto.InventoryDto;
import dk.sdu.inventory_service.model.Order;
import dk.sdu.inventory_service.model.Reservation;
import dk.sdu.inventory_service.model.ReservationEvent;
import dk.sdu.inventory_service.service.InventoryService;
import io.dapr.Topic;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;
    public final String pubSubName = "kafka-pubsub";
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryDto> getAllProducts(){
        return inventoryService.getAllInventory();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Optional<InventoryDto> getProductById(@PathVariable String id){
        return inventoryService.getItemById(id);
    }

    @Topic(name = "On_Products_Reserved", pubsubName = "kafka-pubsub")
    @PostMapping(path = "/reserve", consumes = MediaType.ALL_VALUE)
    public Mono<ResponseEntity> getCheckout(@RequestBody(required = false) CloudEvent<ReservationEvent> cloudEvent) {
        return Mono.fromSupplier(() -> {
            try {
                String reservationId = cloudEvent.getData().getProductId();
                ReservationEvent reservationEvent = cloudEvent.getData();
                logger.info("Subscriber received: " + reservationId);
                var productForReservation = inventoryService.getItemById(reservationId);
                if (productForReservation.isEmpty()){
                    logger.warn("Could not find any products with ID: " + reservationId);
                    return ResponseEntity.notFound().build();
                }

                if (productForReservation.get().getStock() < reservationEvent.getQuantity()){
                    //daprClient.publishEvent(pubSubName, "On_Reservation_Failed", reservationEvent).block();
                    inventoryService.publishEvent(pubSubName, "On_Reservation_Failed", reservationEvent);
                    logger.info("Reservation failed. Not enough stock");
                    return ResponseEntity.notFound().build();
                }

                var subtract = productForReservation.get().getStock();
                subtract -= reservationEvent.getQuantity();
                productForReservation.get().setStock(subtract);
                inventoryService.updateInventory(productForReservation.get());
                logger.info("{} items reserved for product {} for user {} at time {}",
                        reservationEvent.getQuantity(), reservationEvent.getProductId(), reservationEvent.getCustomerId(), new Date().getTime());
                return ResponseEntity.ok().body("Success");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @PostMapping("/addStock")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Products_Released", pubsubName = pubSubName)
    public Mono<ResponseEntity<?>> addStock(@RequestBody Reservation reservation) {
        // Null check for the reservation object
        if (reservation == null || reservation.getProductId() == null || reservation.getQuantity() == 0) {
            return Mono.just(ResponseEntity.badRequest().body("Invalid reservation details"));
        }

        return Mono.fromSupplier(() -> {
            try {
                var product = inventoryService.getItemById(reservation.getProductId());
                if (product.isEmpty()) {
                    logger.info("No product found for: {}", reservation.getProductId());
                    return ResponseEntity.notFound().build();
                }

                var add = product.get().getStock();
                add += reservation.getQuantity();
                product.get().setStock(add);
                inventoryService.updateInventory(product.get());

                logger.info("Order was canceled. Adding {} items back to stock for product {}",
                        reservation.getQuantity(), reservation.getProductId());

                return ResponseEntity.ok().body(product.get());

            } catch (Exception e) {
                logger.error("Error occurred while adding stock: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing the request");
            }
        });
    }

    @GetMapping("/reservation/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getReservationById(@PathVariable String id){
        return ResponseEntity.ok().body(inventoryService.getReservationBy(id));
    }


    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Order_Canceled", pubsubName = pubSubName)
    public Mono<ResponseEntity<?>> cancelOrder(@RequestBody Reservation reservation) {
        // if order is canceled
        return Mono.fromSupplier(() -> {
            var product = inventoryService.getItemById(reservation.getProductId());
            if (product.isEmpty()){
                return ResponseEntity.notFound().build();
            }
            var add = product.get().getStock();
            add += reservation.getQuantity();
            product.get().setStock(add);
            inventoryService.updateInventory(product.get());
            logger.info("Order was canceled. Adding {} items back to stock for product {}",
                    reservation.getQuantity(), reservation.getProductId());
            return ResponseEntity.ok().body(product.get());
        });
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addProductToInventory (@RequestBody InventoryDto inventoryDto){
        inventoryService.addToInventory(inventoryDto);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteProductFromInventory (@PathVariable String id){
        inventoryService.deleteFromInventory(id);
    }

    @PatchMapping("/edit/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateProduct(@PathVariable String id,@RequestBody InventoryDto inventoryDto){
        inventoryDto.setProductId(id);
        inventoryService.updateInventory(inventoryDto);
    }
}


