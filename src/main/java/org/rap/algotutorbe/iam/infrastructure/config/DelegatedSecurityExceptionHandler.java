package org.rap.algotutorbe.iam.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class DelegatedSecurityExceptionHandler implements AccessDeniedHandler, AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public DelegatedSecurityExceptionHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    // Xử lý lỗi 403 Forbidden (Đã login nhưng không có quyền, hoặc lỗi CSRF)
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        resolver.resolveException(request, response, null, accessDeniedException);
    }

    // Xử lý lỗi 401 Unauthorized (Chưa login, không có token)
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        resolver.resolveException(request, response, null, authException);
    }
}

