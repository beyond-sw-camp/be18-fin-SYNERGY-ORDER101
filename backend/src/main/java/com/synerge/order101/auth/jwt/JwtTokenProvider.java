package com.synerge.order101.auth.jwt;

import com.synerge.order101.user.model.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtUtil jwtUtil;
    private static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60L * 30; // 30분
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60L * 60L * 24L; // 1일
    private final RedisTemplate<String, String> redisTemplate;
    private final UserDetailsService userDetailsService;

    public String createAccessToken(Long userId, Role role) {
        Map<String, Object> claims = Map.of("userId", userId, "role",role,"token_type", "access");
        return jwtUtil.createJwtToken(claims, ACCESS_TOKEN_EXPIRATION);
    }

    public String createRefreshToken(Long userId) {
        Map<String, Object> claims = Map.of("userId", userId, "token_type", "refresh");
        String refreshToken = jwtUtil.createJwtToken(claims, REFRESH_TOKEN_EXPIRATION);

        redisTemplate.opsForValue().set(
                "refresh:" + userId, refreshToken, REFRESH_TOKEN_EXPIRATION, TimeUnit.MILLISECONDS);

        return refreshToken;
    }

    public String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public void addBlacklist(String accessToken) {
        String key = "blacklist:" + jwtUtil.getJti(accessToken);

        redisTemplate.opsForValue().set(key, accessToken, ACCESS_TOKEN_EXPIRATION, TimeUnit.MILLISECONDS);
    }

    public void deleteRefreshToken(String accessToken) {
        String userId = jwtUtil.getUserId(accessToken);

        redisTemplate.delete("refresh:" + userId);
    }

    public boolean isUsableAccessToken(String accessToken) {
        if (accessToken == null) {
            log.debug("[JwtTokenProvider] accessToken is null");
            return false;
        }

        boolean valid = jwtUtil.validateToken(accessToken);
        if (!valid) {
            log.debug("[JwtTokenProvider] token validation failed");
            return false;
        }

        if (isBlacklist(accessToken)) {
            log.debug("[JwtTokenProvider] token is blacklisted");
            return false;
        }

        boolean isAccess = false;
        try {
            isAccess = isAccessToken(accessToken);
        } catch (Exception e) {
            log.debug("[JwtTokenProvider] failed to determine token type: {}", e.getMessage());
            return false;
        }

        if (!isAccess) {
            log.debug("[JwtTokenProvider] not an access token");
            return false;
        }

        return true;
    }

    public Authentication createAuthentication(String accessToken) {
        String userId = jwtUtil.getUserId(accessToken);
        log.debug("[JwtTokenProvider] creating authentication for userId={}", userId);
        try {
            UserDetails userDetails= userDetailsService.loadUserByUsername(userId);
            log.debug("[JwtTokenProvider] loaded userDetails class={}, username={}", userDetails.getClass().getName(), userDetails.getUsername());

            Object principal = userDetails;
            // if CustomUserDetails is used, expose the underlying User entity as principal so
            // @AuthenticationPrincipal User injection works in controllers
            if (userDetails instanceof com.synerge.order101.auth.model.service.CustomUserDetails) {
                principal = ((com.synerge.order101.auth.model.service.CustomUserDetails) userDetails).getUser();
            }

            return new UsernamePasswordAuthenticationToken(principal, null, userDetails.getAuthorities());
        } catch (UsernameNotFoundException e) {
            log.warn("[JwtTokenProvider] user not found for id={}", userId);
            throw e;
        }
    }

    private boolean isAccessToken(String accessToken) {
        return jwtUtil.getTokenType(accessToken).equals("access");
    }

    private boolean isBlacklist(String accessToken) {
        String key = "blacklist:" + jwtUtil.getJti(accessToken);
        return redisTemplate.hasKey(key);
    }

    public boolean isValidRefreshToken(String refreshToken) {
        String userId = jwtUtil.getUserId(refreshToken);
        String storedRefreshToken = redisTemplate.opsForValue().get("refresh:" + userId);

        return storedRefreshToken != null && storedRefreshToken.equals(refreshToken);
    }
}
