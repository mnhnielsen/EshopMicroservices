package dk.sdu.product_service.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(value = "brand")
@Builder
@Data
public class Type {
    @Id
    private String id;
    private String bikeType;
}
