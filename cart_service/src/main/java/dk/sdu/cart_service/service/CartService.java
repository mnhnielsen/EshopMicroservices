package dk.sdu.cart_service.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.model.ReservationEvent;
import dk.sdu.cart_service.repository.CartRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


@Service
@RequiredArgsConstructor
@AllArgsConstructor
public class CartService implements CartRepository{
    private final String DAPR_HOST = System.getenv().getOrDefault("DAPR_HOST", "http://localhost");
    private final String DAPR_HTTP_PORT = System.getenv().getOrDefault("DAPR_HTTP_PORT", "3500");
    private static HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveState(Reservation reservation) {
        redisTemplate.opsForList().leftPush(reservation.getCustomerId(),reservation.getItems());
    }

    @Override
    public void getState(String storeName, String id) throws URISyntaxException, IOException, InterruptedException {


        URI baseUrl = new URI(DAPR_HOST+":"+DAPR_HTTP_PORT);
        URI getStateURL = new URI(baseUrl + "/v1.0/state/"+storeName+"/"+id);
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

    @Override
    public void publishEvent(String pubSubName, String topic, ReservationEvent reservationEvent) throws Exception {
        String uri = DAPR_HOST + ":" + DAPR_HTTP_PORT + "/v1.0/publish/" + pubSubName + "/" + topic;
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println(objectMapper.writeValueAsString(reservationEvent));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(reservationEvent)))
                .header("Content-Type", "application/json")
                .build();
        System.out.println(request);
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(uri+" "+response);
    }

}
