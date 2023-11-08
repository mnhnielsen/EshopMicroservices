package dk.sdu.product_service.controller;

import dk.sdu.product_service.model.Reservation;
import dk.sdu.product_service.service.ProductService;
import dk.sdu.product_service.dto.ProductDto;
import io.dapr.client.DaprClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/product")
@RequiredArgsConstructor
public class ProductController {
	private final ProductService productService;
	private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
	private final String pubSubName = "kafka-commonpubsub";
    LocalDateTime date = LocalDateTime.now();

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public List<ProductDto> getAllProducts(){
		return productService.getAllProducts();
	}

	@GetMapping(value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	public Optional<ProductDto> getProduct(@PathVariable String id){
		return productService.getProduct(id);
	}

	@Topic(name = "On_Products_Reserved", pubsubName = pubSubName)
	@PostMapping(consumes = MediaType.ALL_VALUE)
	public ResponseEntity<String> reserve(@RequestBody(required = false) CloudEvent<Reservation> cloudEvent, DaprClient daprClient, Reservation reservation) throws Exception {
		Optional<ProductDto> productsToReserve = productService.getProduct(reservation.getProductId());

		if (productsToReserve.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (productsToReserve.get().getStock() < cloudEvent.getData().getQuantity()){
			daprClient.publishEvent(pubSubName,"On_Products_Reserved",reservation).block();
			logger.info("Not enough stock");
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

        int stock = productsToReserve.get().getStock();
        stock -= reservation.getQuantity();
        productsToReserve.get().setStock(stock);
        logger.info(reservation.getQuantity() + " of the product" + productsToReserve.get().getName() + " has been reserved for the customer " + reservation.getCustomerId() + " at date: " + date);
        return new ResponseEntity<>(HttpStatus.OK);
	}
}
