package com.shopflow.order.rest;

import com.shopflow.common.security.CurrentUser;
import com.shopflow.order.domain.CartItem;
import com.shopflow.order.domain.CartService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 장바구니 REST(UC-08). */
@RestController
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    public record ItemView(Long id, Long productId, int quantity) {
        static ItemView of(CartItem i) {
            return new ItemView(i.getId(), i.getProductId(), i.getQuantity());
        }
    }

    public record AddRequest(@NotNull Long productId, @Positive int quantity) {
    }

    public record QuantityRequest(@Positive int quantity) {
    }

    @GetMapping("/api/cart")
    public List<ItemView> items() {
        return cartService.items(CurrentUser.requireId()).stream().map(ItemView::of).toList();
    }

    @PostMapping("/api/cart/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemView add(@RequestBody AddRequest req) {
        return ItemView.of(cartService.addItem(CurrentUser.requireId(), req.productId(), req.quantity()));
    }

    @PutMapping("/api/cart/items/{itemId}")
    public void updateQuantity(@PathVariable Long itemId, @RequestBody QuantityRequest req) {
        cartService.updateQuantity(CurrentUser.requireId(), itemId, req.quantity());
    }

    @PostMapping("/api/cart/items/{itemId}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long itemId) {
        cartService.removeItem(CurrentUser.requireId(), itemId);
    }
}
