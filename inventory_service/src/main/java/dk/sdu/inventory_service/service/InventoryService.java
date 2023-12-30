package dk.sdu.inventory_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.inventory_service.dto.InventoryDto;
import dk.sdu.inventory_service.mapper.InventoryDtoMapper;
import dk.sdu.inventory_service.model.Event;
import dk.sdu.inventory_service.model.Inventory;
import dk.sdu.inventory_service.model.Reservation;
import dk.sdu.inventory_service.repository.InventoryRepository;
import dk.sdu.inventory_service.repository.ReservationRepository;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryDtoMapper inventoryDtoMapper;
    private final ReservationRepository reservationRepository;
    private final String DAPR_HOST = System.getenv().getOrDefault("DAPR_HOST", "http://localhost");
    private final String DAPR_HTTP_PORT = System.getenv().getOrDefault("DAPR_HTTP_PORT", "3500");
    private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private DaprClient client = new DaprClientBuilder().build();
    public List<InventoryDto> getAllInventory(){
        var inventory = inventoryRepository.findAll();
        return inventory.stream().map(inventoryDtoMapper).collect(Collectors.toList());
    }

    public Optional<InventoryDto> getItemById(String id){
        var inventory = inventoryRepository.findById(id);
        return inventory.map(inventoryDtoMapper);
    }

    public void addToInventory(InventoryDto inventoryDto){
        Inventory inventory = Inventory.builder()
                .name(inventoryDto.getName())
                .description(inventoryDto.getDescription())
                .price(inventoryDto.getPrice())
                .stock(inventoryDto.getStock())
                .bikeType(inventoryDto.getBikeType())
                .build();

        inventoryRepository.save(inventory);
        log.info("Saved product {} to inventory", inventory.getProductId());
    }

    public void deleteFromInventory(String id){
        Inventory inventory = Inventory.builder().build();
        inventoryRepository.deleteById(id);
        log.info("Product {} removed from inventory", inventory.getProductId());
    }

    public void updateInventory(InventoryDto inventoryDto) {
        // Validate that inventory products has a non-null and non-empty ID
        if (inventoryDto.getProductId() == null || inventoryDto.getProductId().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty for updating inventory");
        }

        // Fetch the existing entity from the database
        var inventoryById = inventoryRepository.findById(inventoryDto.getProductId());

        if (inventoryById.isPresent()) {
            // Update the existing entity with the values from the DTO
            Inventory existingInventory = inventoryById.get();
            existingInventory.setName(inventoryDto.getName());
            existingInventory.setDescription(inventoryDto.getDescription());
            existingInventory.setPrice(inventoryDto.getPrice());
            existingInventory.setStock(inventoryDto.getStock());
            existingInventory.setBikeType(inventoryDto.getBikeType());

            // Save the updated entity back to the database
            inventoryRepository.save(existingInventory);

            log.info("Updated product {} in inventory", existingInventory.getProductId());
        } else {
            throw new IllegalArgumentException("No product found with ID: " + inventoryDto.getProductId());
        }
    }

    public Optional<Reservation> getReservationBy(String id){
       return reservationRepository.findById(id);
    }

    public <T> void publishEvent(String pubSubName, String topic, T payload) {
        client.publishEvent(
                pubSubName,
                topic,
                payload).block();

//        try {
//
//            String uri = DAPR_HOST + ":" + DAPR_HTTP_PORT + "/v1.0/publish/" + pubSubName + "/" + topic;
//            ObjectMapper objectMapper = new ObjectMapper();
//            String payloadJson = objectMapper.writeValueAsString(payload);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(uri))
//                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
//                    .header("Content-Type", "application/json")
//                    .build();
//
//            System.out.println(request);
//            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//
//            if (response.statusCode() == HttpStatus.OK.value() || response.statusCode() == HttpStatus.NO_CONTENT.value()) {
//                System.out.println(uri + " " + response.body());
//            } else {
//                System.err.println("Failed to publish event. Status code: " + response.statusCode());
//            }
//        } catch (JsonProcessingException e) {
//            System.err.println("Error converting payload to JSON: " + e.getMessage());
//        } catch (IOException | InterruptedException e) {
//            System.err.println("Error sending HTTP request: " + e.getMessage());
//        }
    }

    public Event[] getEvent(String pubSubName, String path, String topic) {
        try {
            String uri = DAPR_HOST + ":" + DAPR_HTTP_PORT + "/" + path;
            String uriSub = DAPR_HOST + ":" + DAPR_HTTP_PORT + "/dapr/subscribe";

            Event event = Event.builder()
                    .pubSubName(pubSubName)
                    .topic(topic)
                    .route(uriSub)
                    .build();
            log.info("Subscribing to topic {} on pubsub {}", topic, pubSubName);
                return new Event[]{event};
        } catch (Exception e) {
            System.err.println("Error sending HTTP request: " + e.getMessage());
            return null;
        }

    }
}
