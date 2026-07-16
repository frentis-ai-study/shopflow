package com.shopflow.delivery.domain;

import com.shopflow.common.error.DomainException;
import com.shopflow.delivery.repository.DeliveryRepository;
import com.shopflow.order.domain.OrderService;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.domain.SubOrder;
import com.shopflow.order.repository.SubOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * 배송 생성·상태 전이(FR-011) + 주문 상태 집계(FR-022). 소유 판매자만 전이 가능(FR-009).
 */
@Service
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final DeliveryRepository deliveries;
    private final SubOrderRepository subOrders;
    private final OrderService orderService;
    private final Clock clock;

    public DeliveryService(DeliveryRepository deliveries, SubOrderRepository subOrders,
                           OrderService orderService, Clock clock) {
        this.deliveries = deliveries;
        this.subOrders = subOrders;
        this.orderService = orderService;
        this.clock = clock;
    }

    @Transactional
    public Delivery createPending(Long subOrderId) {
        return deliveries.save(new Delivery(subOrderId));
    }

    /** 배송중 전이(PENDING→SHIPPING). 소유 판매자만. */
    @Transactional
    public Delivery ship(Long subOrderId, Long sellerId) {
        SubOrder subOrder = requireOwned(subOrderId, sellerId);
        Delivery delivery = requireDelivery(subOrderId);
        delivery.ship(Instant.now(clock));
        recomputeOrderStatus(subOrder.getOrderId());
        log.info("배송중 전이 subOrder={} seller={}", subOrderId, sellerId);
        return delivery;
    }

    /** 배송완료 전이(SHIPPING→DELIVERED). 완료 시각 기록 → 향후 정산 트리거. */
    @Transactional
    public Delivery deliver(Long subOrderId, Long sellerId) {
        SubOrder subOrder = requireOwned(subOrderId, sellerId);
        Delivery delivery = requireDelivery(subOrderId);
        delivery.deliver(Instant.now(clock));
        recomputeOrderStatus(subOrder.getOrderId());
        log.info("배송완료 전이 subOrder={} seller={}", subOrderId, sellerId);
        return delivery;
    }

    private SubOrder requireOwned(Long subOrderId, Long sellerId) {
        SubOrder subOrder = subOrders.findById(subOrderId)
                .orElseThrow(() -> DomainException.notFound("하위주문을 찾을 수 없습니다"));
        if (!subOrder.getSellerId().equals(sellerId)) {
            throw DomainException.forbidden("본인 주문만 배송 처리할 수 있습니다");
        }
        return subOrder;
    }

    private Delivery requireDelivery(Long subOrderId) {
        return deliveries.findBySubOrderId(subOrderId)
                .orElseThrow(() -> DomainException.notFound("배송 정보를 찾을 수 없습니다"));
    }

    /** 주문에 속한 배송들의 상태를 집계해 주문 상태를 갱신(상호배타 가드). */
    private void recomputeOrderStatus(Long orderId) {
        List<Long> subOrderIds = orderService.subOrdersOf(orderId).stream()
                .map(SubOrder::getId).toList();
        List<Delivery> orderDeliveries = deliveries.findBySubOrderIdIn(subOrderIds);
        if (orderDeliveries.isEmpty()) {
            return;
        }
        long delivered = orderDeliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.DELIVERED).count();
        long shippingOrMore = orderDeliveries.stream()
                .filter(d -> d.getStatus() == DeliveryStatus.SHIPPING || d.getStatus() == DeliveryStatus.DELIVERED)
                .count();
        int total = orderDeliveries.size();

        OrderStatus status;
        if (delivered == total) {
            status = OrderStatus.COMPLETED;
        } else if (shippingOrMore == total) {
            status = OrderStatus.SHIPPED;
        } else if (shippingOrMore > 0) {
            status = OrderStatus.PARTIALLY_SHIPPED;
        } else {
            status = OrderStatus.PAID;
        }
        orderService.updateStatus(orderId, status);
    }
}
