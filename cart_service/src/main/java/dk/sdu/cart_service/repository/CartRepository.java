package dk.sdu.cart_service.repository;

import dk.sdu.cart_service.model.Reservation;
import dk.sdu.cart_service.model.ReservationEvent;

import java.io.IOException;
import java.net.URISyntaxException;

public interface CartRepository {
    void saveState(Reservation reservation) throws Exception;
    void getState(String storeName, String id) throws URISyntaxException, IOException, InterruptedException;
}
