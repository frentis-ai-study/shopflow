package com.shopflow.frontend.page;

import com.shopflow.frontend.client.BackendSession;
import com.shopflow.frontend.client.RestApiClient;
import com.shopflow.frontend.client.dto.OrderDtos;
import com.shopflow.frontend.security.FrontendSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 구매자 주문 조회 화면(UC-10). 본인 주문만 표시(백엔드에서 강제, FR-009/016). */
@Controller
public class OrderPageController {

    private final RestApiClient api;

    public OrderPageController(RestApiClient api) {
        this.api = api;
    }

    @GetMapping("/orders")
    public String list(HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        model.addAttribute("orders", api.myOrders(session));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        model.addAttribute("order", api.orderDetail(session, id));
        model.addAttribute("complete", false);
        return "order-detail";
    }

    @GetMapping("/orders/{id}/complete")
    public String complete(@PathVariable Long id, HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        OrderDtos.Detail order = api.orderDetail(session, id);
        model.addAttribute("order", order);
        return "order-complete";
    }
}
