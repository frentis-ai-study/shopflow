package com.shopflow.delivery.repository;

import com.shopflow.delivery.domain.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.List;
import java.util.Optional;

@RestResource(exported = false)
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findBySubOrderId(Long subOrderId);

    List<Delivery> findBySubOrderIdIn(List<Long> subOrderIds);
}
