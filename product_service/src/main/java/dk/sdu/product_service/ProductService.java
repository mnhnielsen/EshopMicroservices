package dk.sdu.product_service;

import dk.sdu.product_service.dto.ProductDto;
import dk.sdu.product_service.mapper.ProductDtoMapper;
import dk.sdu.product_service.model.Product;
import dk.sdu.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductDtoMapper productDtoMapper;
    public List<ProductDto> getAllProducts(){
        var products = productRepository.findAll();
        return products.stream().map(productDtoMapper).collect(Collectors.toList());
    }

    public Optional<ProductDto> getProduct(String id){
        var product = productRepository.findById(id);
        return product.map(productDtoMapper);
    }

    public void createProduct(ProductDto productDto) {
        Product product = Product.builder()
                .name(productDto.getName())
                .description(productDto.getDescription())
                .price(productDto.getPrice())
                .stock(productDto.getStock())
                .brand(productDto.getBrand())
                .build();

        productRepository.save(product);
        log.info("Product {} is saved", product.getId());
    }
}
