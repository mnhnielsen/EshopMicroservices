package dk.sdu.cart_service.service;
import dk.sdu.cart_service.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {

    private final String DAPR_BASE_URL = "http://localhost:3500/v1.0/bindings/";

    public void saveState(String storeName, String key, byte[] data) throws Exception {
        String saveStateUrl = DAPR_BASE_URL + storeName + "/state/" + key;
        CartRepository.sendHttpRequest(saveStateUrl, "POST", "application/json", data);
    }

    public void publishEvent(String pubSubName, String topic, byte[] data) throws Exception {
        String publishEventUrl = DAPR_BASE_URL + pubSubName + "/bindings/" + topic;
        CartRepository.sendHttpRequest(publishEventUrl, "POST", "application/json", data);
    }

}
