package dk.sdu.product_service.mapper;

import dk.sdu.product_service.dto.ProductDto;
import dk.sdu.product_service.model.Product;
import org.springframework.stereotype.Service;

import java.util.function.Function;
@Service
public class ProductDtoMapper implements Function<Product, ProductDto> {
    @Override
    public ProductDto apply(Product product) {
        return new ProductDto(product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getBrand());
    }
}
