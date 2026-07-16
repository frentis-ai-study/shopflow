package com.shopflow.frontend.page;

import com.shopflow.frontend.client.ApiException;
import com.shopflow.frontend.client.BackendSession;
import com.shopflow.frontend.client.RestApiClient;
import com.shopflow.frontend.security.FrontendSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 판매자 입점(프로필 등록) 게이트(ADR-0009). 판매자 프로필이 없으면 등록 폼으로 유도한다. */
@Controller
public class SellerRegistrationPageController {

    private final RestApiClient api;

    public SellerRegistrationPageController(RestApiClient api) {
        this.api = api;
    }

    /** 백엔드에 "내가 판매자인가"를 직접 묻는 API가 없어, 판매자 전용 조회 호출의 403 여부로 판정한다. */
    static boolean isRegisteredSeller(RestApiClient api, BackendSession session) {
        try {
            api.myProducts(session);
            return true;
        } catch (ApiException e) {
            if (e.status() == 403) {
                return false;
            }
            throw e;
        }
    }

    @GetMapping("/seller")
    public String gate(HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        if (isRegisteredSeller(api, session)) {
            return "redirect:/seller/orders";
        }
        return "seller/register";
    }

    @PostMapping("/seller/register")
    public String register(@RequestParam String sellerType, @RequestParam String storeName,
                           @RequestParam(required = false) String businessRegistrationNumber,
                           @RequestParam(required = false) String representativeName,
                           @RequestParam(required = false) String contactPhone,
                           @RequestParam(required = false) String contactEmail,
                           HttpServletRequest request, Model model, RedirectAttributes redirect) {
        BackendSession session = FrontendSession.get(request);
        try {
            api.registerSeller(session, sellerType, storeName, businessRegistrationNumber,
                    representativeName, contactPhone, contactEmail);
        } catch (ApiException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("storeName", storeName);
            return "seller/register";
        }
        redirect.addFlashAttribute("info", "판매자 등록이 완료되었습니다.");
        return "redirect:/seller/orders";
    }
}
