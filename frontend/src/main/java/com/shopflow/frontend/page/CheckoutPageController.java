package com.shopflow.frontend.page;

import com.shopflow.frontend.client.ApiException;
import com.shopflow.frontend.client.BackendSession;
import com.shopflow.frontend.client.RestApiClient;
import com.shopflow.frontend.client.dto.CartItemDto;
import com.shopflow.frontend.client.dto.CheckoutDtos;
import com.shopflow.frontend.security.FrontendSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 체크아웃·결제 화면(UC-09, FR-015~024). 서버가 발급한 멱등키를 숨은 필드로 전달한다(R3). */
@Controller
public class CheckoutPageController {

    private final RestApiClient api;

    public CheckoutPageController(RestApiClient api) {
        this.api = api;
    }

    @GetMapping("/checkout")
    public String checkoutForm(HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        var items = api.cartItems(session);
        if (items.isEmpty()) {
            return "redirect:/cart";
        }
        long total = items.stream()
                .mapToLong(i -> api.getProduct(i.productId()).priceKrw() * i.quantity())
                .sum();
        CheckoutDtos.Intent intent = api.checkoutIntent(session);
        model.addAttribute("cartTotal", total);
        model.addAttribute("idempotencyKey", intent.idempotencyKey());
        return "checkout";
    }

    @PostMapping("/checkout")
    public String placeOrder(@RequestParam String recipient, @RequestParam String address,
                             @RequestParam String phone, @RequestParam String idempotencyKey,
                             HttpServletRequest request, RedirectAttributes redirect) {
        BackendSession session = FrontendSession.get(request);
        CheckoutDtos.Result result;
        try {
            result = api.checkout(session, recipient, address, phone, idempotencyKey);
        } catch (ApiException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/checkout";
        }
        if ("REJECTED".equals(result.result())) {
            redirect.addFlashAttribute("errorMessage", result.message());
            return "redirect:/checkout";
        }
        return "redirect:/orders/" + result.orderId() + "/complete";
    }
}
