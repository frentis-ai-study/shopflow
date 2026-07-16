package com.shopflow.order.domain;

import com.shopflow.common.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 장바구니 항목(cart_id + product_id 유니크). */
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    protected CartItem() {
    }

    public CartItem(Long cartId, Long productId, int quantity) {
        this.cartId = cartId;
        this.productId = productId;
        changeQuantity(quantity);
    }

    public void changeQuantity(int quantity) {
        if (quantity < 1) {
            throw DomainException.badRequest("수량은 1 이상이어야 합니다");
        }
        this.quantity = quantity;
    }

    public void addQuantity(int delta) {
        changeQuantity(this.quantity + delta);
    }

    public Long getId() {
        return id;
    }

    public Long getCartId() {
        return cartId;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }
}
