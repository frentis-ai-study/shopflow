package com.shopflow.order.repository;

import com.shopflow.order.domain.SubOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;

@RestResource(exported = false)
public interface SubOrderRepository extends JpaRepository<SubOrder, Long> {
    List<SubOrder> findByOrderId(Long orderId);

    List<SubOrder> findBySellerId(Long sellerId);
}
