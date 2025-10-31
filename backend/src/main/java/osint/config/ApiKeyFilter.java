package osint.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    private final String apiKey;

    public ApiKeyFilter(@Value("${API_KEY:}") String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth") || path.equals("/health") || path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }
        if (this.apiKey.isBlank()) {
            // If no API key configured, allow (dev mode)
            filterChain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("X-API-KEY");
        if (header == null || !header.equals(this.apiKey)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid API key");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
