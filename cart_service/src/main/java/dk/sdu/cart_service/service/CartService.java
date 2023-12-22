package dk.sdu.cart_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.repository.CartRepository;
import dk.sdu.cart_service.repository.RedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;


@Slf4j
@Service
public class CartService implements CartRepository {
    private final String DAPR_HOST = System.getenv().getOrDefault("DAPR_HOST", "http://localhost");
    private final String DAPR_HTTP_PORT = System.getenv().getOrDefault("DAPR_HTTP_PORT", "3500");
    private static HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisRepository redisRepository;
    @Autowired
    public CartService(RedisTemplate<String, Object> redisTemplate, RedisRepository redisRepository) {
        this.redisTemplate = redisTemplate;
        this.redisRepository = redisRepository;
    }

    public void saveReservation(String id, Reservation reservation) {
        redisTemplate.opsForValue().set("reservation:" + id, reservation);
    }

    public void addReservation(Reservation reservation) throws Exception {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }

        try {
            // Save to the Redis repository if necessary
            // redisRepository.save(reservation);

            // Save directly using RedisTemplate
            String redisKey = "reservation:" + reservation.getCustomerId();
            redisTemplate.opsForValue().set(redisKey, reservation);
        } catch (DataAccessException e) {
            log.error("Error saving reservation to Redis", e);
            throw new Exception("Error saving reservation to Redis", e);
        }
    }


    @Override
    public void saveState(Reservation reservation) {
        redisTemplate.opsForList().leftPush(reservation.getCustomerId(), reservation.getItems());
    }

    @Override
    public void getState(String storeName, String id) throws URISyntaxException, IOException, InterruptedException {
        URI baseUrl = new URI(DAPR_HOST + ":" + DAPR_HTTP_PORT);
        URI getStateURL = new URI(baseUrl + "/v1.0/state/" + storeName + "/" + id);
        System.out.println(getStateURL);
        httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(getStateURL)
                .header("Content-Type", "application/json")
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body() + response);
    }

    public Optional<Object> getCart(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            log.error("Customer ID is null or empty");
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        try {
            var res = redisRepository.findById(customerId);
            return Optional.ofNullable(res);
        } catch (DataAccessException e) {
             log.error("Error accessing Redis repository", e);
            throw new RuntimeException("Error retrieving data from Redis", e);
        }
    }

    public <T> void publishEvent(String pubSubName, String topic, T payload) {
        try {
            String uri = DAPR_HOST + ":" + DAPR_HTTP_PORT + "/v1.0/publish/" + pubSubName + "/" + topic;
            ObjectMapper objectMapper = new ObjectMapper();
            String payloadJson = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                    .header("Content-Type", "application/json")
                    .build();

            System.out.println(request);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.OK.value() || response.statusCode() == HttpStatus.NO_CONTENT.value()) {
                System.out.println(uri + " " + response.body());
            } else {
                System.err.println("Failed to publish event. Status code: " + response.statusCode());
            }
        } catch (JsonProcessingException e) {
            System.err.println("Error converting payload to JSON: " + e.getMessage());
        } catch (IOException | InterruptedException e) {
            System.err.println("Error sending HTTP request: " + e.getMessage());
        }
    }
}
