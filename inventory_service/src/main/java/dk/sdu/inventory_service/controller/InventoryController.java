package dk.sdu.inventory_service.controller;

import dk.sdu.inventory_service.dto.InventoryDto;
import dk.sdu.inventory_service.model.Reservation;
import dk.sdu.inventory_service.model.ReservationEvent;
import dk.sdu.inventory_service.service.InventoryService;
import io.dapr.Topic;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    public List<InventoryDto> getAllProducts() {
        return inventoryService.getAllInventory();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Optional<InventoryDto> getProductById(@PathVariable String id) {
        return inventoryService.getItemById(id);
    }

    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Products_Reserved", pubsubName = "kafka-pubsub")
    @PostMapping(path = "/reserve", consumes = MediaType.ALL_VALUE)
    public Mono<ResponseEntity<?>> reserveProduct(@RequestBody(required = false) CloudEvent<ReservationEvent> cloudEvent) {
        return Mono.fromSupplier(() -> {
            try {
                DaprClient daprClient = new DaprClientBuilder().build();
                String reservationId = cloudEvent.getData().getProductId();
                ReservationEvent reservationEvent = cloudEvent.getData();
                logger.info("Subscriber received: " + reservationId);
                var productForReservation = inventoryService.getItemById(reservationId);
                if (productForReservation.isEmpty()){
                    logger.warn("Could not find any products with ID: " + reservationId);
                    return ResponseEntity.notFound().build();
                }

                if (productForReservation.get().getStock() < reservationEvent.getQuantity()){
                    daprClient.publishEvent(pubSubName, "On_Reservation_Failed", reservationEvent).block();
                    //inventoryService.publishEvent(pubSubName, "On_Reservation_Failed", reservationEvent);
                    logger.info("Reservation failed. Not enough stock");
                    return ResponseEntity.notFound().build();
                }

                var subtractStock = productForReservation.get().getStock();
                subtractStock -= reservationEvent.getQuantity();
                productForReservation.get().setStock(subtractStock);
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
    public Mono<ResponseEntity<?>> addStock(@RequestBody CloudEvent<ReservationEvent> cloudEvent) {
        return Mono.fromSupplier(() -> {
            try {
                String reservationId = cloudEvent.getData().getProductId();
                var product = inventoryService.getItemById(reservationId);
                if (product.isEmpty()) {
                    logger.info("No product found for: {}", reservationId);
                    return ResponseEntity.notFound().build();
                }

                var add = product.get().getStock();
                add += cloudEvent.getData().getQuantity();
                product.get().setStock(add);
                inventoryService.updateInventory(product.get());

                logger.info("Item was removed from cart. Adding {} items back to stock for product {}",
                        cloudEvent.getData().getQuantity(), cloudEvent.getData().getProductId());

                return ResponseEntity.ok().body(product.get());

            } catch (Exception e) {
                logger.error("Error occurred while adding stock: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing the request");
            }
        });
    }

    @GetMapping("/reservation/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getReservationById(@PathVariable String id) {
        return ResponseEntity.ok().body(inventoryService.getReservationBy(id));
    }


    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Order_Canceled", pubsubName = pubSubName)
    public Mono<ResponseEntity<?>> cancelOrder(@RequestBody CloudEvent<ReservationEvent> cloudEvent) {
        return Mono.fromSupplier(() -> {
            String reservationId = cloudEvent.getData().getProductId();
            var product = inventoryService.getItemById(reservationId);
            if (product.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            var add = product.get().getStock();
            add += cloudEvent.getData().getQuantity();
            product.get().setStock(add);
            inventoryService.updateInventory(product.get());
            logger.info("Order was canceled. Adding {} items back to stock for product {}",
                    cloudEvent.getData().getQuantity(), cloudEvent.getData().getProductId());
            return ResponseEntity.ok().body(product.get());
        });
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addProductToInventory(@RequestBody InventoryDto inventoryDto) {
        inventoryService.addToInventory(inventoryDto);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteProductFromInventory(@PathVariable String id) {
        inventoryService.deleteFromInventory(id);
    }

    @PatchMapping("/edit/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateProduct(@PathVariable String id, @RequestBody InventoryDto inventoryDto) {
        inventoryDto.setProductId(id);
        inventoryService.updateInventory(inventoryDto);
    }
}
