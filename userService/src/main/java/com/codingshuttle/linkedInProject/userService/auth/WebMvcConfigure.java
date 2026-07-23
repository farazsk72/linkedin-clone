package com.codingshuttle.linkedInProject.userService.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfigure implements WebMvcConfigurer {

    @Autowired
    private RequestInterceptor requestInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Only /profile/** is authenticated - /auth/signup and /auth/login are public
        // and never carry an X-User-Id header.
        registry.addInterceptor(requestInterceptor)
                .addPathPatterns("/profile/**");
        WebMvcConfigurer.super.addInterceptors(registry);
    }
}
