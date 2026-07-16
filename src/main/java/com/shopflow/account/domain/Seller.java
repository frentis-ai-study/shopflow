package com.shopflow.account.domain;

import com.shopflow.common.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 판매자 프로필(ADR-0009). User와 1:1. 사업자·법인이면 사업자등록번호·대표자가 필수.
 */
@Entity
@Table(name = "sellers")
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "seller_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SellerType sellerType;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "business_registration_number")
    private String businessRegistrationNumber;

    @Column(name = "representative_name")
    private String representativeName;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SellerStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Seller() {
    }

    public Seller(Long userId, SellerType sellerType, String storeName,
                  String businessRegistrationNumber, String representativeName,
                  String contactPhone, String contactEmail, Instant createdAt) {
        validate(sellerType, storeName, businessRegistrationNumber, representativeName);
        this.userId = userId;
        this.sellerType = sellerType;
        this.storeName = storeName;
        this.businessRegistrationNumber = businessRegistrationNumber;
        this.representativeName = representativeName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.status = SellerStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    /** 유형별 필수값 검증(ADR-0009). 사업자·법인이면 사업자등록번호·대표자 필수. */
    static void validate(SellerType type, String storeName, String brn, String repName) {
        if (storeName == null || storeName.isBlank()) {
            throw DomainException.badRequest("스토어 이름은 필수입니다");
        }
        if (type.requiresBusinessInfo()) {
            if (brn == null || brn.isBlank()) {
                throw DomainException.badRequest("사업자·법인은 사업자등록번호가 필요합니다");
            }
            if (!brn.replaceAll("-", "").matches("\\d{10}")) {
                throw DomainException.badRequest("사업자등록번호 형식이 올바르지 않습니다(10자리 숫자)");
            }
            if (repName == null || repName.isBlank()) {
                throw DomainException.badRequest("사업자·법인은 대표자명이 필요합니다");
            }
        }
    }

    public boolean isActive() {
        return status == SellerStatus.ACTIVE;
    }

    public void suspend() {
        this.status = SellerStatus.SUSPENDED;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public SellerType getSellerType() {
        return sellerType;
    }

    public String getStoreName() {
        return storeName;
    }

    public SellerStatus getStatus() {
        return status;
    }
}
