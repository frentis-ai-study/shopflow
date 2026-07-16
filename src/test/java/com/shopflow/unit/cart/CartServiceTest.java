package com.shopflow.unit.cart;

import com.shopflow.common.error.DomainException;
import com.shopflow.order.domain.Cart;
import com.shopflow.order.domain.CartItem;
import com.shopflow.order.domain.CartService;
import com.shopflow.order.repository.CartItemRepository;
import com.shopflow.order.repository.CartRepository;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 장바구니 담기·수량·upsert 누적 — FR-014. */
class CartServiceTest {

    private final CartRepository carts = mock(CartRepository.class);
    private final CartItemRepository items = mock(CartItemRepository.class);
    private final ProductService productService = mock(ProductService.class);
    private final CartService service = new CartService(carts, items, productService);

    private Product onSaleProduct() {
        return new Product(10L, "사과", null, 1000, 5, null); // ON_SALE
    }

    @Test
    void 수량_0이하_담기는_거부() {
        assertThatThrownBy(() -> service.addItem(1L, 100L, 0))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 같은_상품_담으면_수량_누적() {
        when(productService.get(100L)).thenReturn(onSaleProduct());
        Cart cart = new Cart(1L);
        when(carts.findByBuyerId(1L)).thenReturn(Optional.of(cart));
        CartItem existing = new CartItem(cart.getId(), 100L, 2);
        when(items.findByCartIdAndProductId(any(), any())).thenReturn(Optional.of(existing));

        service.addItem(1L, 100L, 3);

        assertThat(existing.getQuantity()).isEqualTo(5); // 2 + 3
    }

    @Test
    void 새_상품은_새_항목으로_저장() {
        when(productService.get(100L)).thenReturn(onSaleProduct());
        Cart cart = new Cart(1L);
        when(carts.findByBuyerId(1L)).thenReturn(Optional.of(cart));
        when(items.findByCartIdAndProductId(any(), any())).thenReturn(Optional.empty());
        when(items.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItem item = service.addItem(1L, 100L, 2);

        assertThat(item.getProductId()).isEqualTo(100L);
        assertThat(item.getQuantity()).isEqualTo(2);
    }
}
