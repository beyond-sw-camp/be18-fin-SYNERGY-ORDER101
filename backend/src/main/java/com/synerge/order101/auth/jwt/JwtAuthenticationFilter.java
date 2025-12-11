package com.synerge.order101.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtUtil jwtUtil;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. HttpServletRequest 객체에서 토큰을 추출
        String authHeader = request.getHeader("Authorization");
        log.debug("[JwtAuthenticationFilter] Authorization header: {}", authHeader);
        String token = jwtTokenProvider.resolveToken(authHeader);
        log.debug("[JwtAuthenticationFilter] Resolved token: {}", token);

        try {
            // 2. 추출한 토큰의 무결성과 유효성을 검증 & 블랙리스트 확인 & 엑세스 토큰 확인
            if (jwtTokenProvider.isUsableAccessToken(token)) {
                // 3. Authentication 객체를 생성
                Authentication authentication = jwtTokenProvider.createAuthentication(token);

                // 4. Authentication 객체를 SecurityContext 객체에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("[JwtAuthenticationFilter] Authentication set: {}", authentication.getPrincipal());
            } else {
                log.debug("[JwtAuthenticationFilter] Token not usable or missing");
            }
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // JWT 서명 검증 실패 - 토큰이 다른 키로 서명됨 (환경 변경 등)
            log.warn("[JwtAuthenticationFilter] JWT signature validation failed: {}", e.getMessage());
            sendUnauthorizedResponse(response, "INVALID_TOKEN_SIGNATURE", "JWT token signature is invalid. Please login again.");
            return;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // 토큰 만료
            log.warn("[JwtAuthenticationFilter] JWT token expired: {}", e.getMessage());
            sendUnauthorizedResponse(response, "TOKEN_EXPIRED", "JWT token has expired. Please login again.");
            return;
        } catch (Exception e) {
            // 기타 JWT 검증 오류
            log.error("[JwtAuthenticationFilter] Failed to authenticate token: {}", e.getMessage(), e);
            sendUnauthorizedResponse(response, "INVALID_TOKEN", "JWT token validation failed.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String errorCode, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
            "{\"code\":\"%s\",\"message\":\"%s\"}",
            errorCode, errorMessage
        ));
    }
}
