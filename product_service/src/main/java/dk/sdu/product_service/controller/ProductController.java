package dk.sdu.product_service.controller;

import dk.sdu.product_service.ProductService;
import dk.sdu.product_service.dto.ProductDto;
import dk.sdu.product_service.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProductDto> getAllProducts(){
        return productService.getAllProducts();
    }

    @GetMapping(value = "/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Optional<ProductDto> getProduct(@PathVariable String id){
        return productService.getProduct(id);
    }

    // this functionality should be moved to the inventory service
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createProduct(@RequestBody ProductDto productDto) {
        productService.createProduct(productDto);
    }
}
