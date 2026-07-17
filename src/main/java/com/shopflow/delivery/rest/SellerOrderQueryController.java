package com.shopflow.delivery.rest;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.delivery.domain.Delivery;
import com.shopflow.delivery.repository.DeliveryRepository;
import com.shopflow.order.domain.OrderLine;
import com.shopflow.order.domain.SubOrder;
import com.shopflow.order.repository.OrderLineRepository;
import com.shopflow.order.repository.SubOrderRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 판매자 하위주문 목록 조회 REST(UC-11, FR-010). 본인 판매자에게 배정된 것만 노출한다. */
@RestController
public class SellerOrderQueryController {

    private final SubOrderRepository subOrders;
    private final OrderLineRepository orderLines;
    private final DeliveryRepository deliveries;
    private final SellerService sellerService;

    public SellerOrderQueryController(SubOrderRepository subOrders, OrderLineRepository orderLines,
                                      DeliveryRepository deliveries, SellerService sellerService) {
        this.subOrders = subOrders;
        this.orderLines = orderLines;
        this.deliveries = deliveries;
        this.sellerService = sellerService;
    }

    public record LineView(String productName, long unitPriceKrw, int quantity) {
        static LineView of(OrderLine l) {
            return new LineView(l.getProductNameSnapshot(), l.getUnitPriceKrwSnapshot(), l.getQuantity());
        }
    }

    public record SellerSubOrderView(Long subOrderId, Long orderId, long subtotalKrw,
                                     String deliveryStatus, List<LineView> lines) {
    }

    @GetMapping("/api/seller/orders")
    public List<SellerSubOrderView> myOrders() {
        Seller seller = sellerService.requireActiveSeller(CurrentUser.requireId());
        List<SubOrder> mine = subOrders.findBySellerId(seller.getId());
        return mine.stream().map(this::toView).toList();
    }

    private SellerSubOrderView toView(SubOrder subOrder) {
        List<LineView> lines = orderLines.findBySubOrderId(subOrder.getId()).stream()
                .map(LineView::of).toList();
        String status = deliveries.findBySubOrderId(subOrder.getId())
                .map(Delivery::getStatus).map(Enum::name).orElse("PENDING");
        return new SellerSubOrderView(subOrder.getId(), subOrder.getOrderId(),
                subOrder.getSubtotalKrw(), status, lines);
    }
}
