package dk.sdu.orderservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaymentDto {
    public String orderId;
    public String customerId;
    public String orderStatus;
}
