package com.shopflow.account.rest;

import com.shopflow.account.domain.User;
import com.shopflow.account.domain.UserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 회원가입 REST (UC-01, POST /api/signup). */
@RestController
@RequestMapping("/api/signup")
public class SignupController {

    private final UserService userService;

    public SignupController(UserService userService) {
        this.userService = userService;
    }

    public record SignupRequest(
            @NotBlank String email,
            @NotBlank String password,
            @NotBlank String displayName) {
    }

    public record SignupResponse(Long userId, String email, String displayName) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@RequestBody SignupRequest req) {
        User user = userService.signup(req.email(), req.password(), req.displayName());
        return new SignupResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
