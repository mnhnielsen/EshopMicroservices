package dk.sdu.inventory_service.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Event {
    @JsonAlias("pubsubname")
    private String pubSubName;
    private String topic;
    private String route;
}
