package dk.sdu.shippingservice.controller;

import dk.sdu.shippingservice.model.ShippingDto;
import io.dapr.Topic;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import io.dapr.client.domain.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("api/shipping")
@Slf4j
public class ShippingController {
    public final String pubSubName = "kafka-pubsub";

    public ShippingController() {

    }

    @PostMapping(value = "/ship")
    @ResponseStatus(HttpStatus.OK)
    @Topic(name = "On_Payment_Received", pubsubName = pubSubName)
    public Mono<ResponseEntity<String>>shipOrder(@RequestBody CloudEvent<ShippingDto> cloudEvent) {
        return Mono.fromCallable(() -> {
            try {
                DaprClient client = new DaprClientBuilder().build();
                log.info("READY FOR SHIPPING: Have Order: {}, from Customer: {}, with status: {}", cloudEvent.getData().getOrderId(),
                        cloudEvent.getData().getCustomerId(), cloudEvent.getData().getOrderStatus());
                UUID uuid = UUID.randomUUID();
                String trackingId = uuid.toString();
                var event = cloudEvent.getData();
                client.publishEvent(pubSubName, "On_Order_Shipped", event).block();
                log.info("A tracking id: {} has been generated", trackingId);
                log.info("Order shipped for Customer: {}, with Order: {}",
                        cloudEvent.getData().getCustomerId(), cloudEvent.getData().getOrderId());
                return ResponseEntity.ok().body("Order shipped");
            } catch (Exception e) {
                log.error("Error occurred while publishing event", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while publishing event");
            }
        });
    }
}
