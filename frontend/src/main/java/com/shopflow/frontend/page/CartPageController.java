package com.shopflow.frontend.page;

import com.shopflow.frontend.client.ApiException;
import com.shopflow.frontend.client.BackendSession;
import com.shopflow.frontend.client.RestApiClient;
import com.shopflow.frontend.client.dto.CartItemDto;
import com.shopflow.frontend.client.dto.ProductDto;
import com.shopflow.frontend.security.FrontendSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 장바구니 화면(UC-08). 판매자별로 묶어 보여준다(체크아웃과 동일한 분리 원칙 미리 안내). */
@Controller
public class CartPageController {

    private final RestApiClient api;

    public CartPageController(RestApiClient api) {
        this.api = api;
    }

    public record CartLine(Long itemId, Long productId, String name, long priceKrw, int quantity,
                           String imageUrl, long lineTotal) {
    }

    public record SellerGroup(Long sellerId, List<CartLine> items, long subtotal) {
    }

    @GetMapping("/cart")
    public String view(HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        List<CartItemDto> items = api.cartItems(session);
        Map<Long, SellerGroup> grouped = groupBySeller(items);
        model.addAttribute("sellerGroups", grouped.values());
        model.addAttribute("cartTotal", grouped.values().stream().mapToLong(SellerGroup::subtotal).sum());
        model.addAttribute("empty", items.isEmpty());
        return "cart";
    }

    private Map<Long, SellerGroup> groupBySeller(List<CartItemDto> items) {
        Map<Long, List<CartLine>> bySeller = new LinkedHashMap<>();
        for (CartItemDto item : items) {
            ProductDto p = api.getProduct(item.productId());
            CartLine line = new CartLine(item.id(), p.id(), p.name(), p.priceKrw(), item.quantity(),
                    p.imageUrl(), p.priceKrw() * item.quantity());
            bySeller.computeIfAbsent(p.sellerId(), k -> new ArrayList<>()).add(line);
        }
        Map<Long, SellerGroup> result = new LinkedHashMap<>();
        bySeller.forEach((sellerId, lines) -> {
            long subtotal = lines.stream().mapToLong(CartLine::lineTotal).sum();
            result.put(sellerId, new SellerGroup(sellerId, lines, subtotal));
        });
        return result;
    }

    @PostMapping("/cart/items")
    public String add(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity,
                      HttpServletRequest request, RedirectAttributes redirect) {
        BackendSession session = FrontendSession.get(request);
        if (session == null) {
            redirect.addAttribute("next", "/products/" + productId);
            return "redirect:/login";
        }
        try {
            api.addCartItem(session, productId, quantity);
        } catch (ApiException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/products/" + productId;
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{id}")
    public String updateQuantity(@PathVariable Long id, @RequestParam int quantity,
                                 HttpServletRequest request) {
        BackendSession session = FrontendSession.get(request);
        api.updateCartItemQuantity(session, id, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/items/{id}/delete")
    public String remove(@PathVariable Long id, HttpServletRequest request) {
        BackendSession session = FrontendSession.get(request);
        api.removeCartItem(session, id);
        return "redirect:/cart";
    }
}
