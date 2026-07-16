package com.shopflow.order.repository;

import com.shopflow.order.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Optional;

@RestResource(exported = false)
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByBuyerId(Long buyerId);
}
