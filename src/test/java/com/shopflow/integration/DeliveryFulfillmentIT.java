package com.shopflow.integration;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.account.domain.SellerType;
import com.shopflow.account.domain.UserService;
import com.shopflow.common.domain.Address;
import com.shopflow.common.error.DomainException;
import com.shopflow.delivery.domain.DeliveryService;
import com.shopflow.delivery.domain.DeliveryStatus;
import com.shopflow.order.domain.CartService;
import com.shopflow.order.domain.CheckoutService;
import com.shopflow.order.domain.CheckoutService.CheckoutResult;
import com.shopflow.order.domain.Order;
import com.shopflow.order.domain.OrderStatus;
import com.shopflow.order.domain.SubOrder;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.repository.SubOrderRepository;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 판매자 배송 이행 통합(US4): 배송중→배송완료→주문 집계 COMPLETED, 소유권 차단(SC-006/007). */
@SpringBootTest
@ActiveProfiles("test")
class DeliveryFulfillmentIT {

    @Autowired UserService userService;
    @Autowired SellerService sellerService;
    @Autowired ProductService productService;
    @Autowired CartService cartService;
    @Autowired CheckoutService checkoutService;
    @Autowired DeliveryService deliveryService;
    @Autowired SubOrderRepository subOrderRepository;
    @Autowired OrderRepository orderRepository;

    @Test
    void 배송중_배송완료_전이시_주문_COMPLETED_그리고_타판매자_차단() {
        long sellerUid = userService.signup("s-" + UUID.randomUUID() + "@shop.com", "password123", "판매자").getId();
        Seller seller = sellerService.register(sellerUid, SellerType.INDIVIDUAL, "store-" + UUID.randomUUID(),
                null, null, null, null);
        Product product = productService.register(seller.getId(), "상품-" + UUID.randomUUID(), null, 1000, 5, null);

        long buyerId = userService.signup("b-" + UUID.randomUUID() + "@shop.com", "password123", "구매자").getId();
        cartService.addItem(buyerId, product.getId(), 2);
        CheckoutResult result = checkoutService.checkout(buyerId, new Address("수령", "주소", "010"),
                UUID.randomUUID().toString());

        SubOrder subOrder = subOrderRepository.findByOrderId(result.orderId()).get(0);

        // 타 판매자 배송 시도 → 차단
        long otherUid = userService.signup("o-" + UUID.randomUUID() + "@shop.com", "password123", "타판매자").getId();
        Seller other = sellerService.register(otherUid, SellerType.INDIVIDUAL, "other-" + UUID.randomUUID(),
                null, null, null, null);
        assertThatThrownBy(() -> deliveryService.ship(subOrder.getId(), other.getId()))
                .isInstanceOf(DomainException.class);

        // 소유 판매자: 배송중 → 배송완료
        deliveryService.ship(subOrder.getId(), seller.getId());
        assertThat(orderRepository.findById(result.orderId()).map(Order::getStatus))
                .contains(OrderStatus.SHIPPED);

        var delivery = deliveryService.deliver(subOrder.getId(), seller.getId());
        assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);

        // 단일 판매자 주문 → 전부 배송완료 → COMPLETED
        assertThat(orderRepository.findById(result.orderId()).map(Order::getStatus))
                .contains(OrderStatus.COMPLETED);
    }
}
