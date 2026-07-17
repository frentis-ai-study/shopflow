package com.shopflow.frontend.page;

import com.shopflow.frontend.client.ApiException;
import com.shopflow.frontend.client.RestApiClient;
import com.shopflow.frontend.client.dto.ProductDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** 상품 탐색·상세 화면(UC-03, UC-04). 공개 화면. */
@Controller
public class ProductPageController {

    private final RestApiClient api;

    public ProductPageController(RestApiClient api) {
        this.api = api;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/products";
    }

    @GetMapping("/products")
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("products", api.browseProducts(q));
        model.addAttribute("query", q == null ? "" : q);
        return "products";
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("product", api.getProduct(id));
        } catch (ApiException e) {
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "product-detail";
    }
}
