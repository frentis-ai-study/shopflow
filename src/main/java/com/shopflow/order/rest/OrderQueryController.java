package com.shopflow.order.rest;

import com.shopflow.common.error.DomainException;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.delivery.domain.Delivery;
import com.shopflow.delivery.repository.DeliveryRepository;
import com.shopflow.order.domain.Order;
import com.shopflow.order.domain.SubOrder;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.repository.SubOrderRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 구매자 주문 조회 REST(UC-10). 본인 주문만(FR-009/016). */
@RestController
public class OrderQueryController {

    private final OrderRepository orders;
    private final SubOrderRepository subOrders;
    private final DeliveryRepository deliveries;

    public OrderQueryController(OrderRepository orders, SubOrderRepository subOrders,
                                DeliveryRepository deliveries) {
        this.orders = orders;
        this.subOrders = subOrders;
        this.deliveries = deliveries;
    }

    public record OrderSummary(Long id, long totalKrw, String status, String placedAt) {
    }

    public record SubOrderView(Long subOrderId, String sellerStoreName, long subtotalKrw, String deliveryStatus) {
    }

    public record OrderDetail(Long id, long totalKrw, String status, List<SubOrderView> subOrders) {
    }

    @GetMapping("/api/orders")
    public List<OrderSummary> myOrders() {
        Long buyerId = CurrentUser.requireId();
        return orders.findByBuyerIdOrderByIdDesc(buyerId).stream()
                .map(o -> new OrderSummary(o.getId(), o.getTotalKrw(), o.getStatus().name(),
                        String.valueOf(o.getStatus())))
                .toList();
    }

    @GetMapping("/api/orders/{id}")
    public OrderDetail detail(@PathVariable Long id) {
        Long buyerId = CurrentUser.requireId();
        Order order = orders.findById(id)
                .orElseThrow(() -> DomainException.notFound("주문을 찾을 수 없습니다"));
        if (!order.getBuyerId().equals(buyerId)) {
            throw DomainException.forbidden("본인 주문만 조회할 수 있습니다");
        }
        List<SubOrderView> views = subOrders.findByOrderId(id).stream()
                .map(this::toView)
                .toList();
        return new OrderDetail(order.getId(), order.getTotalKrw(), order.getStatus().name(), views);
    }

    private SubOrderView toView(SubOrder so) {
        String deliveryStatus = deliveries.findBySubOrderId(so.getId())
                .map(Delivery::getStatus)
                .map(Enum::name)
                .orElse("PENDING");
        return new SubOrderView(so.getId(), so.getSellerStoreNameSnapshot(), so.getSubtotalKrw(), deliveryStatus);
    }
}
