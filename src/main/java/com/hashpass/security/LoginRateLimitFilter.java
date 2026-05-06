package com.hashpass.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    public LoginRateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String email = normalize(request.getParameter("email"));
        String clientKey = "login:" + resolveClientIp(request) + "|" + email;
        if (rateLimitService.tryConsume(clientKey, 5, 15)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendRedirect("/password-login?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&rateLimited=1");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}