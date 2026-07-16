package com.shopflow.order.domain;

import com.shopflow.common.error.DomainException;
import com.shopflow.order.repository.CartItemRepository;
import com.shopflow.order.repository.CartRepository;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductStatus;
import com.shopflow.product.domain.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 장바구니 담기·수정·삭제(FR-014). 담기 시점엔 재고 안내만, 확정 판정은 체크아웃 선점에서. */
@Service
public class CartService {

    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductService productService;

    public CartService(CartRepository carts, CartItemRepository items, ProductService productService) {
        this.carts = carts;
        this.items = items;
        this.productService = productService;
    }

    @Transactional
    public Cart getOrCreateCart(Long buyerId) {
        return carts.findByBuyerId(buyerId).orElseGet(() -> carts.save(new Cart(buyerId)));
    }

    /** 상품 담기(같은 상품이면 수량 누적, upsert). */
    @Transactional
    public CartItem addItem(Long buyerId, Long productId, int quantity) {
        if (quantity < 1) {
            throw DomainException.badRequest("수량은 1 이상이어야 합니다");
        }
        Product product = productService.get(productId);
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw DomainException.conflict("판매 중인 상품이 아닙니다");
        }
        Cart cart = getOrCreateCart(buyerId);
        return items.findByCartIdAndProductId(cart.getId(), productId)
                .map(existing -> {
                    existing.addQuantity(quantity);
                    return existing;
                })
                .orElseGet(() -> items.save(new CartItem(cart.getId(), productId, quantity)));
    }

    @Transactional
    public void updateQuantity(Long buyerId, Long itemId, int quantity) {
        CartItem item = requireOwnedItem(buyerId, itemId);
        item.changeQuantity(quantity);
    }

    @Transactional
    public void removeItem(Long buyerId, Long itemId) {
        CartItem item = requireOwnedItem(buyerId, itemId);
        items.delete(item);
    }

    @Transactional(readOnly = true)
    public List<CartItem> items(Long buyerId) {
        Cart cart = getOrCreateCart(buyerId);
        return items.findByCartId(cart.getId());
    }

    @Transactional
    public void clear(Long buyerId) {
        Cart cart = getOrCreateCart(buyerId);
        items.deleteByCartId(cart.getId());
    }

    private CartItem requireOwnedItem(Long buyerId, Long itemId) {
        CartItem item = items.findById(itemId)
                .orElseThrow(() -> DomainException.notFound("장바구니 항목을 찾을 수 없습니다"));
        Cart cart = getOrCreateCart(buyerId);
        if (!item.getCartId().equals(cart.getId())) {
            throw DomainException.forbidden("본인 장바구니만 수정할 수 있습니다");
        }
        return item;
    }
}
