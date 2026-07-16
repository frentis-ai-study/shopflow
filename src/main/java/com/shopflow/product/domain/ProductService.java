package com.shopflow.product.domain;

import com.shopflow.common.error.DomainException;
import com.shopflow.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 상품 등록·수정·판매상태·조회(FR-006~008, 012, 013). 소유권은 sellerId로 강제(FR-009). */
@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    @Transactional
    public Product register(Long sellerId, String name, String description,
                            long priceKrw, int stock, String imageUrl) {
        return products.save(new Product(sellerId, name, description, priceKrw, stock, imageUrl));
    }

    @Transactional
    public Product updateBasics(Long productId, Long sellerId, String name, String description,
                                long priceKrw, String imageUrl) {
        Product product = requireOwned(productId, sellerId);
        product.updateBasics(name, description, priceKrw, imageUrl);
        return product;
    }

    @Transactional
    public Product changeStock(Long productId, Long sellerId, int newStock) {
        Product product = requireOwned(productId, sellerId);
        product.changeStock(newStock);
        return product;
    }

    @Transactional
    public Product changeStatus(Long productId, Long sellerId, ProductStatus status) {
        Product product = requireOwned(productId, sellerId);
        product.changeStatus(status);
        return product;
    }

    @Transactional(readOnly = true)
    public List<Product> browse(String query) {
        if (query == null || query.isBlank()) {
            return products.findByStatus(ProductStatus.ON_SALE);
        }
        return products.findByStatusAndNameContainingIgnoreCase(ProductStatus.ON_SALE, query.trim());
    }

    @Transactional(readOnly = true)
    public Product get(Long productId) {
        return products.findById(productId)
                .orElseThrow(() -> DomainException.notFound("상품을 찾을 수 없습니다"));
    }

    @Transactional(readOnly = true)
    public List<Product> mine(Long sellerId) {
        return products.findBySellerId(sellerId);
    }

    private Product requireOwned(Long productId, Long sellerId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> DomainException.notFound("상품을 찾을 수 없습니다"));
        product.requireOwnedBy(sellerId);
        return product;
    }
}
