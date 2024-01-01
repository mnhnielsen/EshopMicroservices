package dk.sdu.orderservice.dto;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
public class PaymentDto {
    private String customerId;
    private String orderId;
    private String orderStatus;
}
