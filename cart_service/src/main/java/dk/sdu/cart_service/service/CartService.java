package dk.sdu.cart_service.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class CartService implements CartRepository{

    private final String DAPR_BASE_URL = "http://localhost:3500/v1.0";
    private final String DAPR_HOST = System.getenv().getOrDefault("DAPR_HOST", "http://localhost");
    private final String DAPR_HTTP_PORT = System.getenv().getOrDefault("DAPR_HTTP_PORT", "3500");
    private static HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public void saveState(String storeName, byte[] data) throws URISyntaxException {
        URI baseUrl = new URI(DAPR_HOST+":"+DAPR_HTTP_PORT);
        URI stateStoreUrl = new URI(baseUrl + "/v1.0/state/"+ storeName);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(stateStoreUrl)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(data)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(stateStoreUrl);
            System.out.println(response + " hello????");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public Optional<HttpResponse<String>> getState(String storeName, String id) throws URISyntaxException, IOException, InterruptedException {
        URI baseUrl = new URI(DAPR_HOST+":"+DAPR_HTTP_PORT);
        URI getStateURL = new URI(baseUrl + "/v1.0/state/"+storeName+"/"+id);
        httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(getStateURL)
                .build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(getStateURL);
        System.out.println(response);
        return Optional.of(Objects.requireNonNull(response));
    }

    @Override
    public void publishEvent(String pubSubName, String topic, byte[] data) throws Exception {
        String uri = DAPR_HOST + ":" + DAPR_HTTP_PORT + "/v1.0/publish/" + pubSubName + "/" + topic;
        ObjectMapper objectMapper = new ObjectMapper();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(data)))
                .header("Content-Type", "application/json")
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(uri);

    }

}
