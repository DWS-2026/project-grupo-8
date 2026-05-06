package com.hashpass.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimited rateLimited = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(),
                RateLimited.class);
        if (rateLimited == null) {
            rateLimited = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RateLimited.class);
        }

        if (rateLimited == null) {
            return true;
        }

        String clientKey = resolveClientKey(request, rateLimited.key());
        String endpointKey = handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName();
        String bucketKey = endpointKey + "|" + clientKey + "|" + rateLimited.requests() + "|" + rateLimited.minutes();

        if (rateLimitService.tryConsume(bucketKey, rateLimited.requests(), rateLimited.minutes())) {
            return true;
        }

        log.warn("SECURITY_EVENT=RATE_LIMIT_EXCEEDED client={} endpoint={} method={} ip={}",
                maskIdentifier(clientKey),
                endpointKey,
                request.getMethod(),
                resolveClientIp(request));

        response.setStatus(429);
        response.setHeader("Retry-After", Integer.toString(rateLimited.minutes() * 60));
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Demasiadas peticiones. Intenta de nuevo más tarde.\"}");
        return false;
    }

    private String resolveClientKey(HttpServletRequest request, String keyMode) {
        String principal = request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();
        String clientIp = resolveClientIp(request);

        if ("user".equalsIgnoreCase(keyMode) && principal != null && !principal.isBlank()) {
            return "user:" + principal.trim().toLowerCase();
        }
        if ("ip".equalsIgnoreCase(keyMode)) {
            return "ip:" + clientIp;
        }
        if (principal != null && !principal.isBlank()) {
            return "user:" + principal.trim().toLowerCase();
        }
        return "ip:" + clientIp;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private String maskIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return trimmed.substring(0, 2) + "***" + trimmed.substring(trimmed.length() - 2);
    }
}