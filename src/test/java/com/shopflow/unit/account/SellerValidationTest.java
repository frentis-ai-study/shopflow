package com.shopflow.unit.account;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerStatus;
import com.shopflow.account.domain.SellerType;
import com.shopflow.common.error.DomainException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 판매자 유형별 필수값 검증 — ADR-0009. */
class SellerValidationTest {

    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void 개인은_사업자정보_없이_등록가능() {
        assertThatCode(() -> new Seller(1L, SellerType.INDIVIDUAL, "홍길동 스토어",
                null, null, null, null, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void 개인사업자는_사업자등록번호_필수() {
        assertThatThrownBy(() -> new Seller(1L, SellerType.SOLE_PROPRIETOR, "상점",
                null, "대표", null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("사업자등록번호");
    }

    @Test
    void 법인은_대표자_필수() {
        assertThatThrownBy(() -> new Seller(1L, SellerType.CORPORATION, "주식회사",
                "1234567890", null, null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("대표자");
    }

    @Test
    void 사업자등록번호_형식오류_거부() {
        assertThatThrownBy(() -> new Seller(1L, SellerType.SOLE_PROPRIETOR, "상점",
                "123", "대표", null, null, NOW))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("형식");
    }

    @Test
    void 유효한_법인_등록시_ACTIVE() {
        Seller seller = new Seller(1L, SellerType.CORPORATION, "주식회사",
                "123-45-67890", "김대표", "010-0000-0000", "biz@corp.com", NOW);
        assertThat(seller.getStatus()).isEqualTo(SellerStatus.ACTIVE);
    }
}
