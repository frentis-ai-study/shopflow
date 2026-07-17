package com.shopflow.frontend.page;

import com.shopflow.frontend.client.BackendSession;
import com.shopflow.frontend.client.RestApiClient;
import com.shopflow.frontend.security.FrontendSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** 판매자 주문·배송 처리 화면(UC-11, UC-12, FR-010/011). */
@Controller
public class SellerOrderPageController {

    private final RestApiClient api;

    public SellerOrderPageController(RestApiClient api) {
        this.api = api;
    }

    @ModelAttribute("sellerSection")
    public String sellerSection() {
        return "orders";
    }

    @GetMapping("/seller/orders")
    public String list(HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        if (!SellerRegistrationPageController.isRegisteredSeller(api, session)) {
            return "redirect:/seller";
        }
        model.addAttribute("subOrders", api.sellerOrders(session));
        return "seller/orders";
    }

    @PostMapping("/seller/orders/{id}/ship")
    public String ship(@PathVariable Long id, HttpServletRequest request) {
        api.ship(FrontendSession.get(request), id);
        return "redirect:/seller/orders";
    }

    @PostMapping("/seller/orders/{id}/deliver")
    public String deliver(@PathVariable Long id, HttpServletRequest request) {
        api.deliver(FrontendSession.get(request), id);
        return "redirect:/seller/orders";
    }
}
