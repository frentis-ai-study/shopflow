package com.shopflow.product.rest;

import com.shopflow.account.domain.Seller;
import com.shopflow.account.domain.SellerService;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductService;
import com.shopflow.product.domain.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 상품 REST(커스텀). 조회는 공개, 쓰기는 판매자 소유권 인가(FR-009, 검토 보고서 #6).
 * 응답은 DTO로만 노출해 reserved/version 등 내부 필드 과다노출을 막는다.
 */
@RestController
public class ProductController {

    private final ProductService productService;
    private final SellerService sellerService;

    public ProductController(ProductService productService, SellerService sellerService) {
        this.productService = productService;
        this.sellerService = sellerService;
    }

    // ---- 응답 DTO ----
    public record ProductView(Long id, String name, String description, long priceKrw,
                              boolean inStock, String imageUrl, String status, Long sellerId) {
        static ProductView of(Product p) {
            return new ProductView(p.getId(), p.getName(), p.getDescription(), p.getPriceKrw(),
                    p.availableStock() > 0, p.getImageUrl(), p.getStatus().name(), p.getSellerId());
        }
    }

    // ---- 요청 DTO ----
    public record CreateRequest(@NotBlank String name, String description,
                                @PositiveOrZero long priceKrw, @PositiveOrZero int stock,
                                String imageUrl) {
    }

    public record UpdateRequest(@NotBlank String name, String description,
                                @PositiveOrZero long priceKrw, String imageUrl) {
    }

    public record StockRequest(@PositiveOrZero int stock) {
    }

    public record StatusRequest(@NotNull ProductStatus status) {
    }

    // ================= 구매자/공개 조회 =================
    @GetMapping("/api/products")
    public List<ProductView> browse(@RequestParam(name = "q", required = false) String q) {
        return productService.browse(q).stream().map(ProductView::of).toList();
    }

    @GetMapping("/api/products/{id}")
    public ProductView detail(@PathVariable Long id) {
        return ProductView.of(productService.get(id));
    }

    // ================= 판매자 관리 =================
    @GetMapping("/api/seller/products")
    public List<ProductView> myProducts() {
        Long sellerId = currentSellerId();
        return productService.mine(sellerId).stream().map(ProductView::of).toList();
    }

    @PostMapping("/api/seller/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@RequestBody CreateRequest req) {
        Long sellerId = currentSellerId();
        return ProductView.of(productService.register(sellerId, req.name(), req.description(),
                req.priceKrw(), req.stock(), req.imageUrl()));
    }

    @PutMapping("/api/seller/products/{id}")
    public ProductView update(@PathVariable Long id, @RequestBody UpdateRequest req) {
        Long sellerId = currentSellerId();
        return ProductView.of(productService.updateBasics(id, sellerId, req.name(),
                req.description(), req.priceKrw(), req.imageUrl()));
    }

    @PutMapping("/api/seller/products/{id}/stock")
    public ProductView changeStock(@PathVariable Long id, @RequestBody StockRequest req) {
        Long sellerId = currentSellerId();
        return ProductView.of(productService.changeStock(id, sellerId, req.stock()));
    }

    @PostMapping("/api/seller/products/{id}/status")
    public ProductView changeStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        Long sellerId = currentSellerId();
        return ProductView.of(productService.changeStatus(id, sellerId, req.status()));
    }

    /** 현재 로그인 사용자의 활성 판매자 id. */
    private Long currentSellerId() {
        Seller seller = sellerService.requireActiveSeller(CurrentUser.requireId());
        return seller.getId();
    }
}
