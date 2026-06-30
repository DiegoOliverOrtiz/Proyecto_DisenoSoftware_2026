package edu.esi.ds.esientradas.config;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.esi.ds.esientradas.services.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(20)
public class PublicEndpointRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Value("${app.rate-limit.public.max-requests:120}")
    private int maxRequests;

    @Value("${app.rate-limit.public.window-seconds:60}")
    private long windowSeconds;

    public PublicEndpointRateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (isPublicEndpoint(request)) {
            rateLimiterService.check("public-endpoint", clientIp(request), maxRequests, Duration.ofSeconds(windowSeconds));
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().startsWith("/busqueda/");
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
