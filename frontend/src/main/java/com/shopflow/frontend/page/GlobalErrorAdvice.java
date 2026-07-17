package com.shopflow.frontend.page;

import com.shopflow.frontend.client.ApiException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** 백엔드 API 오류를 화면에 한글 메시지로 보여준다(민감정보 비노출). */
@ControllerAdvice
public class GlobalErrorAdvice {

    @ExceptionHandler(ApiException.class)
    public String handleApi(ApiException e, Model model) {
        model.addAttribute("statusCode", e.status());
        model.addAttribute("errorMessage", e.getMessage());
        return "error-page";
    }
}
