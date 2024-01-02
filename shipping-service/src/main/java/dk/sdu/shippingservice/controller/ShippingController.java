package dk.sdu.shippingservice.controller;

import dk.sdu.shippingservice.model.PaymentDto;
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
                var event = cloudEvent.getData();
                var paymentConfirm = new PaymentDto(event.getOrderId(), event.getCustomerId(), "Shipped");
                log.info("READY FOR SHIPPING: " + paymentConfirm.getOrderId());
                UUID uuid = UUID.randomUUID();
                String trackingId = uuid.toString();
                client.publishEvent(pubSubName, "On_Order_Shipped", paymentConfirm).block();
                log.info("A tracking id: {} has been generated", trackingId);
                log.info("Order shipped with id: {}", trackingId);
                return ResponseEntity.ok().body("Order shipped");
            } catch (Exception e) {
                log.error("Error occurred while publishing event", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while publishing event");
            }
        });
    }
}
