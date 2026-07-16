package com.shopflow.product.repository;

import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // ===== 재고 원자적 조건부 UPDATE (ADR-0004, 검토 보고서 #1). @Version 미사용, 네이티브 전용. =====

    /** 선점: 판매중이고 가용재고가 충분할 때만 reserved 증가. 성공 시 1행. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE products SET reserved = reserved + :qty "
            + "WHERE id = :id AND status = 'ON_SALE' AND (stock - reserved) >= :qty", nativeQuery = true)
    int reserveStock(@Param("id") Long id, @Param("qty") int qty);

    /** 확정 차감: 선점을 실재고 차감으로 전환. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE products SET stock = stock - :qty, reserved = reserved - :qty "
            + "WHERE id = :id AND reserved >= :qty", nativeQuery = true)
    int confirmStock(@Param("id") Long id, @Param("qty") int qty);

    /** 선점 해제: reserved만 복원. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE products SET reserved = reserved - :qty "
            + "WHERE id = :id AND reserved >= :qty", nativeQuery = true)
    int releaseStock(@Param("id") Long id, @Param("qty") int qty);
}
