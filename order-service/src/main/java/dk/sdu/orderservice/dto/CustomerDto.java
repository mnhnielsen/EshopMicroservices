package dk.sdu.orderservice.dto;

import lombok.*;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDto {
    public String name;
    public String email;
    public String address;
}
