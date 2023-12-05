package dk.sdu.cart_service.repository;

import dk.sdu.cart_service.model.Reservation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.Optional;

public interface CartRepository {
    void saveState(String storeName, byte[] data) throws Exception;
    void publishEvent(String pubSubName, String topic, byte[] data) throws Exception;

    Optional<HttpResponse<String>> getState(String storeName, String id) throws URISyntaxException, IOException, InterruptedException;
    static void sendHttpRequest(String url, String method, String contentType, byte[] data) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL apiUrl = new URL(url);
            connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", contentType);
            connection.setDoOutput(true);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(data);
            }
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP error code " + connection.getResponseCode());
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
