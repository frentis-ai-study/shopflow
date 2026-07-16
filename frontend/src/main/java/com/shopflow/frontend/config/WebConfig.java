package com.shopflow.frontend.config;

import com.shopflow.frontend.security.AuthRequiredInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 보호 경로 인터셉터 등록. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthRequiredInterceptor())
                .addPathPatterns("/cart/**", "/checkout/**", "/orders/**",
                        "/seller/**");
    }
}
