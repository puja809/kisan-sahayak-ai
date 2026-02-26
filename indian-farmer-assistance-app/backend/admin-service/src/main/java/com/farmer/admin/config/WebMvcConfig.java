package com.farmer.admin.config;

import com.farmer.admin.interceptor.AuditLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for registering interceptors.
 * Requirements: 21.11, 22.7
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditLoggingInterceptor auditLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditLoggingInterceptor)
                .addPathPatterns("/api/v1/admin/**");
    }
}
