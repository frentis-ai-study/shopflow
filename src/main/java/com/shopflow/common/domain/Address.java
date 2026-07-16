package com.shopflow.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

/** 배송지 값객체(임베더블). 수령인·주소·연락처. */
@Embeddable
public record Address(
        @NotBlank @Column(name = "ship_recipient") String recipient,
        @NotBlank @Column(name = "ship_address") String address,
        @NotBlank @Column(name = "ship_phone") String phone
) {
}
