package dk.sdu.inventory_service.controller;

import dk.sdu.inventory_service.dto.InventoryDto;
import dk.sdu.inventory_service.model.Reservation;
import dk.sdu.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/reserve")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> reserveProduct(@RequestBody Reservation reservation){
        var productForReservation = inventoryService.getItemById(reservation.getProductId());
        if (productForReservation.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        if (productForReservation.get().getStock() < reservation.getQuantity()){
            inventoryService.publishEvent(pubSubName, "On_Reservation_Failed", reservation);
            logger.info("Reservation failed. Not enough stock");
            return ResponseEntity.notFound().build();
        }

        var subtract = productForReservation.get().getStock();
        subtract -= reservation.getQuantity();
        productForReservation.get().setStock(subtract);
        inventoryService.updateInventory(productForReservation.get());
        logger.info("{} items reserved for product {} for user {} at time {}",
                reservation.getQuantity(), reservation.getProductId(), reservation.getCustomerId(), new Date().getTime());
        return ResponseEntity.ok().body(productForReservation.get());
    }

    @PostMapping("/addStock")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> addStock(@RequestBody Reservation reservation){
        // if product is removed from cart
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
    }

    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> cancelOrder(@RequestBody Reservation reservation) {
        // if order is canceled
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
        inventoryDto.setId(id);
        inventoryService.updateInventory(inventoryDto);
    }
}
