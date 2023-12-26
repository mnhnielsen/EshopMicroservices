package dk.sdu.inventory_service.model;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventData<T> {
    private T data;

}
