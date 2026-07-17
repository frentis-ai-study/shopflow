package com.shopflow.frontend.page;

import com.shopflow.frontend.client.ApiException;
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
import org.springframework.web.bind.annotation.RequestParam;

/** 판매자 상품 관리 화면(UC-05~07, FR-006~009). */
@Controller
public class SellerProductPageController {

    private final RestApiClient api;

    public SellerProductPageController(RestApiClient api) {
        this.api = api;
    }

    @ModelAttribute("sellerSection")
    public String sellerSection() {
        return "products";
    }

    @GetMapping("/seller/products")
    public String list(HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        if (!SellerRegistrationPageController.isRegisteredSeller(api, session)) {
            return "redirect:/seller";
        }
        model.addAttribute("products", api.myProducts(session));
        return "seller/products";
    }

    @GetMapping("/seller/products/new")
    public String newForm() {
        return "seller/product-form";
    }

    @PostMapping("/seller/products")
    public String create(@RequestParam String name, @RequestParam(required = false) String description,
                         @RequestParam long priceKrw, @RequestParam int stock,
                         @RequestParam(required = false) String imageUrl,
                         HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        try {
            api.createProduct(session, name, description, priceKrw, stock, imageUrl);
        } catch (ApiException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "seller/product-form";
        }
        return "redirect:/seller/products";
    }

    @GetMapping("/seller/products/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", api.getProduct(id));
        return "seller/product-edit";
    }

    @PostMapping("/seller/products/{id}")
    public String update(@PathVariable Long id, @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam long priceKrw, @RequestParam(required = false) String imageUrl,
                         HttpServletRequest request, Model model) {
        BackendSession session = FrontendSession.get(request);
        try {
            api.updateProduct(session, id, name, description, priceKrw, imageUrl);
        } catch (ApiException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("product", api.getProduct(id));
            return "seller/product-edit";
        }
        return "redirect:/seller/products";
    }

    @PostMapping("/seller/products/{id}/stock")
    public String changeStock(@PathVariable Long id, @RequestParam int stock, HttpServletRequest request) {
        api.changeStock(FrontendSession.get(request), id, stock);
        return "redirect:/seller/products/" + id + "/edit";
    }

    @PostMapping("/seller/products/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam String status, HttpServletRequest request) {
        api.changeStatus(FrontendSession.get(request), id, status);
        return "redirect:/seller/products";
    }
}
