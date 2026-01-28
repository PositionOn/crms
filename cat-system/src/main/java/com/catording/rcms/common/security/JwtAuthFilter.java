package com.catording.rcms.common.security;

import com.catording.rcms.common.exception.BizException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenService jwt;

    public JwtAuthFilter(JwtTokenService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring("Bearer ".length());
            try {
                Claims c = jwt.parse(token);
                String username = c.getSubject();
                String role = String.valueOf(c.get("role"));
                Long operatorId = Long.valueOf(String.valueOf(c.get("operatorId")));
                Authentication a = new UsernamePasswordAuthenticationToken(
                        new OperatorPrincipal(operatorId, username, role),
                        token,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(a);
            } catch (Exception e) {
                throw BizException.unauthorized("Token 无效或已过期");
            }
        }
        filterChain.doFilter(request, response);
    }
}

