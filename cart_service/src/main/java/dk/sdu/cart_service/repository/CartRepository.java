package dk.sdu.cart_service.repository;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public interface CartRepository {
    public void saveState(String storeName, String key, byte[] data) throws Exception;
    public void publishEvent(String pubSubName, String topic, byte[] data) throws Exception;
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
