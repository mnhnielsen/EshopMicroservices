package dk.sdu.cart_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.repository.RedisRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;


@Slf4j
@Service
public class CartService {
    private final String DAPR_HOST = System.getenv().getOrDefault("DAPR_HOST", "http://localhost");
    private final String DAPR_HTTP_PORT = System.getenv().getOrDefault("DAPR_HTTP_PORT", "3500");
    private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @Qualifier("redisTemplate")
    @Autowired
    private final RedisTemplate redisTemplate;
    public static final String HASH_KEY = "Reservation";

    public CartService(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Reservation getCartById(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            log.error("Customer ID is null or empty");
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        try {
            Reservation res = (Reservation) redisTemplate.opsForHash().get(HASH_KEY, customerId);
            return res;
        } catch (DataAccessException e) {
            log.error("Error accessing Redis repository", e);
            throw new RuntimeException("Error retrieving data from Redis", e);
        }
    }

    public void saveReservation(Reservation reservation) {
        if (reservation == null) {
            log.error("Reservation is null");
            throw new IllegalArgumentException("Reservation cannot be null");
        }
        try {
            log.info(HASH_KEY);
            redisTemplate.opsForHash().put(HASH_KEY,reservation.getCustomerId(),reservation);
        } catch (DataAccessException e){
            log.error("Error saving reservation {}", e.getMessage());
            throw new RuntimeException("Error saving reservation", e);
        }
    }

    public void removeCart(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            log.error("Customer ID is null or empty");
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        try {
            Reservation res = (Reservation) redisTemplate.opsForHash().get(HASH_KEY, customerId);
            if (res != null) {
                redisTemplate.opsForHash().delete(HASH_KEY, customerId, res);
                log.info("Reservation deleted: {}", res);
            }
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
