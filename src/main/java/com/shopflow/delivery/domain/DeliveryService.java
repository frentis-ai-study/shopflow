package com.shopflow.delivery.domain;

import com.shopflow.delivery.repository.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 배송 생성·상태 전이(FR-011). 결제 완료 시 SubOrder마다 PENDING 생성. */
@Service
public class DeliveryService {

    private final DeliveryRepository deliveries;

    public DeliveryService(DeliveryRepository deliveries) {
        this.deliveries = deliveries;
    }

    @Transactional
    public Delivery createPending(Long subOrderId) {
        return deliveries.save(new Delivery(subOrderId));
    }
}
