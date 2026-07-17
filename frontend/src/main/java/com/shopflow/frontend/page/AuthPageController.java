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

/** 가입·로그인·로그아웃 화면(UC-01, UC-02). */
@Controller
public class AuthPageController {

    private final RestApiClient api;

    public AuthPageController(RestApiClient api) {
        this.api = api;
    }

    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@RequestParam String email, @RequestParam String password,
                         @RequestParam String displayName, Model model, RedirectAttributes redirect) {
        try {
            api.signup(email, password, displayName);
        } catch (ApiException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("displayName", displayName);
            return "signup";
        }
        redirect.addFlashAttribute("info", "가입이 완료되었습니다. 로그인해 주세요.");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String next, Model model) {
        model.addAttribute("next", next);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        @RequestParam(required = false) String next,
                        HttpServletRequest request, Model model) {
        BackendSession session;
        try {
            session = api.login(email, password);
        } catch (ApiException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("next", next);
            return "login";
        }
        FrontendSession.put(request, session);
        return "redirect:" + (next != null && !next.isBlank() ? next : "/products");
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        BackendSession session = FrontendSession.get(request);
        if (session != null) {
            api.logout(session);
        }
        FrontendSession.clear(request);
        return "redirect:/products";
    }
}
