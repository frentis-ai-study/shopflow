package com.shopflow.product.domain;

import com.shopflow.common.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * 상품. 재고 무결식 {@code 0 <= reserved <= stock}(DB CHECK로 최후 방어).
 *
 * <p><b>동시성 주의(검토 보고서 #1, ADR-0004)</b>: {@code reserved}는 재고 컨텍스트가 네이티브
 * 조건부 UPDATE로만 변경한다. 그래서 이 엔티티에서 {@code reserved}는 <b>읽기 전용</b>
 * (insertable/updatable=false)으로 두어, 판매자 상품 수정 시 stale 값으로 덮어쓰지 않는다.
 * 판매자 편집(name/price/stock/status)은 {@code @Version} 낙관적 락으로 보호한다.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "price_krw", nullable = false)
    private long priceKrw;

    @Column(nullable = false)
    private int stock;

    // 재고 컨텍스트 전용(네이티브 UPDATE). JPA는 읽기만 한다.
    @Column(name = "reserved", insertable = false, updatable = false)
    private int reserved;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Version
    private long version;

    protected Product() {
    }

    public Product(Long sellerId, String name, String description, long priceKrw, int stock, String imageUrl) {
        if (name == null || name.isBlank()) {
            throw DomainException.badRequest("상품명은 필수입니다");
        }
        if (priceKrw < 0) {
            throw DomainException.badRequest("가격은 0 이상이어야 합니다");
        }
        if (stock < 0) {
            throw DomainException.badRequest("재고는 0 이상이어야 합니다");
        }
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.priceKrw = priceKrw;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.status = ProductStatus.ON_SALE;
    }

    /** 소유권 검증(FR-009). */
    public void requireOwnedBy(Long sellerId) {
        if (!this.sellerId.equals(sellerId)) {
            throw DomainException.forbidden("본인 상품만 관리할 수 있습니다");
        }
    }

    public void updateBasics(String name, String description, long priceKrw, String imageUrl) {
        if (name == null || name.isBlank()) {
            throw DomainException.badRequest("상품명은 필수입니다");
        }
        if (priceKrw < 0) {
            throw DomainException.badRequest("가격은 0 이상이어야 합니다");
        }
        this.name = name;
        this.description = description;
        this.priceKrw = priceKrw;
        this.imageUrl = imageUrl;
    }

    /** 재고 하향은 현재 선점량 미만으로 내릴 수 없다(검토 보고서 #10). */
    public void changeStock(int newStock) {
        if (newStock < 0) {
            throw DomainException.badRequest("재고는 0 이상이어야 합니다");
        }
        if (newStock < this.reserved) {
            throw DomainException.conflict("선점된 수량(" + reserved + ")보다 적게 재고를 내릴 수 없습니다");
        }
        this.stock = newStock;
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }

    public int availableStock() {
        return stock - reserved;
    }

    public Long getId() {
        return id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getPriceKrw() {
        return priceKrw;
    }

    public int getStock() {
        return stock;
    }

    public int getReserved() {
        return reserved;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public ProductStatus getStatus() {
        return status;
    }
}
