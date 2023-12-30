package dk.sdu.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class OrderEvent {
    private String customerId;
    private List<Item> items;
}
