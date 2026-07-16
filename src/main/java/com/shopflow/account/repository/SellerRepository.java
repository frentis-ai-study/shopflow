package com.shopflow.account.repository;

import com.shopflow.account.domain.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Optional;

/** 판매자 프로필 리포지토리. 내부용(REST 미노출). 프로필 조회는 커스텀 엔드포인트로 통제. */
@RestResource(exported = false)
public interface SellerRepository extends JpaRepository<Seller, Long> {

    Optional<Seller> findByUserId(Long userId);

    boolean existsByBusinessRegistrationNumber(String businessRegistrationNumber);
}
