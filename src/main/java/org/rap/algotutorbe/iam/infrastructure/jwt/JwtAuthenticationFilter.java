package org.rap.algotutorbe.iam.infrastructure.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.rap.algotutorbe.iam.infrastructure.SecurityUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final HandlerExceptionResolver exceptionResolver;


    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService,
                                   @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractAccessTokenFromCookie(request);

        try {
            if (isValidToken(token)) {
                authenticateUser(request, token);
            }
        } catch (UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            exceptionResolver.resolveException(request, response, null, ex);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateUser(HttpServletRequest request, String token) {
        String userName = jwtUtil.getUserNameFromToken(token);
        SecurityUser user = (SecurityUser) userDetailsService.loadUserByUsername(userName);

        if (user.isEnabled()) {
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }


    private boolean isValidToken(String token) {
        return token != null && jwtUtil.isValidToken(token);
    }

    private String extractAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals("access-token"))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

}