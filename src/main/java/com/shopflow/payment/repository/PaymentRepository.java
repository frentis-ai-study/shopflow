package com.shopflow.payment.repository;

import com.shopflow.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

@RestResource(exported = false)
public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
