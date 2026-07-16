package com.shopflow.unit.order;

import com.shopflow.common.domain.Address;
import com.shopflow.order.domain.Order;
import com.shopflow.order.domain.OrderLine;
import com.shopflow.order.domain.OrderService;
import com.shopflow.order.domain.OrderService.LineSpec;
import com.shopflow.order.domain.OrderService.PlacedOrder;
import com.shopflow.order.domain.SubOrder;
import com.shopflow.order.repository.OrderLineRepository;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.repository.SubOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 주문 분리·스냅샷·총액 산출 — FR-021, FR-023. */
class OrderSplitSnapshotTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final SubOrderRepository subOrders = mock(SubOrderRepository.class);
    private final OrderLineRepository orderLines = mock(OrderLineRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC);
    private final OrderService service = new OrderService(orders, subOrders, orderLines, clock);

    @Test
    void 두_판매자_상품은_하위주문_2개로_분리되고_총액이_맞다() {
        // 저장 시 id를 부여하는 것처럼 흉내
        when(orders.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        AtomicLong subId = new AtomicLong(100);
        when(subOrders.save(any(SubOrder.class))).thenAnswer(inv -> {
            SubOrder s = inv.getArgument(0);
            // 리플렉션 없이 id 확인이 어려우므로 순번만 사용
            return s;
        });
        when(orderLines.save(any(OrderLine.class))).thenAnswer(inv -> inv.getArgument(0));

        List<LineSpec> lines = List.of(
                new LineSpec(1L, 10L, "A상점", "사과", 1000, 2),   // 2000
                new LineSpec(2L, 20L, "B상점", "배", 1500, 1)      // 1500
        );

        PlacedOrder placed = service.place(1L, new Address("수령", "주소", "010"), lines);

        assertThat(placed.totalKrw()).isEqualTo(3500);
        assertThat(placed.subOrders()).hasSize(2);
        assertThat(placed.subOrders())
                .extracting(OrderService.SubOrderRef::sellerId)
                .containsExactlyInAnyOrder(10L, 20L);
    }
}
