package com.shopflow.order.repository;

import com.shopflow.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

@RestResource(exported = false)
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerIdOrderByIdDesc(Long buyerId);
}
