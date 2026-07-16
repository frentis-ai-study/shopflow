package com.shopflow.account.domain;

import com.shopflow.account.repository.SellerRepository;
import com.shopflow.common.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** 판매자 프로필 등록·조회(ADR-0009). 유형별 검증은 Seller 생성자에서 강제. */
@Service
public class SellerService {

    private final SellerRepository sellers;
    private final UserService userService;
    private final Clock clock;

    public SellerService(SellerRepository sellers, UserService userService, Clock clock) {
        this.sellers = sellers;
        this.userService = userService;
        this.clock = clock;
    }

    /** 판매자 입점(프로필 생성 + SELLER 역할 부여). 사업자등록번호 중복 불가. */
    @Transactional
    public Seller register(Long userId, SellerType type, String storeName,
                           String businessRegistrationNumber, String representativeName,
                           String contactPhone, String contactEmail) {
        if (sellers.findByUserId(userId).isPresent()) {
            throw DomainException.conflict("이미 판매자로 등록된 계정입니다");
        }
        if (type.requiresBusinessInfo()
                && sellers.existsByBusinessRegistrationNumber(businessRegistrationNumber)) {
            throw DomainException.conflict("이미 등록된 사업자등록번호입니다");
        }
        Seller seller = new Seller(userId, type, storeName, businessRegistrationNumber,
                representativeName, contactPhone, contactEmail, Instant.now(clock));
        Seller saved = sellers.save(seller);
        userService.grantSellerRole(userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Seller requireActiveSeller(Long userId) {
        Seller seller = sellers.findByUserId(userId)
                .orElseThrow(() -> DomainException.forbidden("판매자만 접근할 수 있습니다"));
        if (!seller.isActive()) {
            throw DomainException.forbidden("정지된 판매자입니다");
        }
        return seller;
    }
}
