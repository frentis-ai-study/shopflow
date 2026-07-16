package com.shopflow.order.repository;

import com.shopflow.order.domain.OrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

@RestResource(exported = false)
public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {
    List<OrderLine> findBySubOrderId(Long subOrderId);
}
