package com.shopflow.product.repository;

import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

/**
 * 상품 리포지토리.
 *
 * <p>검토 보고서(#3/#6)에 따라 자동 REST 노출은 하지 않는다(exported=false). 조회·검색·쓰기는
 * 모두 {@code ProductController} 커스텀 REST로 통제해 reserved/version 등 내부 필드 과다노출과
 * 무결성 우회를 막는다.
 */
@RestResource(exported = false)
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByStatusAndNameContainingIgnoreCase(ProductStatus status, String name);

    List<Product> findBySellerId(Long sellerId);
}
