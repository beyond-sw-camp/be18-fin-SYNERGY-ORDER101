package com.synerge.order101.auth.jwt;

import com.synerge.order101.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {
    private final String issuer;
    private final SecretKey secretKey;

    public JwtUtil(JwtProperties jwtProperties) {

        this.issuer = jwtProperties.getIssuer();
        String secret = jwtProperties.getSecret();
        
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        
        // 공백/개행 제거 (AWS Parameter Store에서 공백이 포함될 수 있음)
        secret = secret.trim();
        
        log.info("[JwtUtil] Loaded JWT secret length: {} chars", secret.length());
        
        byte[] keyBytes;
        
        // Base64로 인코딩된 시크릿 처리 (권장)
        try {
            keyBytes = Base64.getDecoder().decode(secret);
            log.info("[JwtUtil] JWT secret decoded from Base64, key bytes length: {} bytes", keyBytes.length);
        } catch (IllegalArgumentException e) {
            // Base64가 아니면 평문 UTF-8로 처리 (하위 호환성)
            log.warn("[JwtUtil] JWT secret is not Base64 encoded, using as plain UTF-8 text");
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            log.info("[JwtUtil] JWT secret converted to UTF-8 bytes, length: {} bytes", keyBytes.length);
        }
        
        // 키가 256비트(32바이트) 미만이면 경고
        if (keyBytes.length < 32) {
            log.warn("[JwtUtil] JWT secret key is only {} bytes. HMAC-SHA256 requires at least 32 bytes for security.", keyBytes.length);
        }
        
        // 디버깅: 키의 해시값 출력 (서버간 일관성 확인용)
        String keyHash = Integer.toHexString(java.util.Arrays.hashCode(keyBytes));
        log.info("[JwtUtil] Secret key hash (for consistency check): {}", keyHash);
        
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("[JwtUtil] SecretKey initialized successfully");
    }


    public String createJwtToken(Map<String, Object> claims, long expiration) {

        return Jwts.builder()
                .setHeaderParam("typ","JWT")
                .setClaims(claims)
                .setId(Long.toHexString(System.nanoTime()))
                .setIssuer(issuer) // 발급 주체
                .setIssuedAt(new Date()) // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 만료 시간
                .signWith(secretKey) // 서명을 생성
                .compact();

    }

    // 클래임에서 username을 추출하는 메소드
    public String getUserId(String token) {
        Object userIdObj = getClaims(token).get("userId");
        if (userIdObj == null) return null;
        if (userIdObj instanceof Number) {
            return String.valueOf(((Number) userIdObj).longValue());
        }
        return userIdObj.toString();
    }

    // 클래임에서 토큰 타입(Token Type)을 추출하는 메소드
    public String getTokenType(String token) {

        return getClaims(token).get("token_type", String.class);
    }

    // 클래임에서 JTI(JWT ID)를 추출하는 메소드
    public String getJti(String token) {

        return getClaims(token).getId();
    }

    // 클래임에서 발급 시간(IssuedAt)을 추출하는 메소드
    public long getIssuedAt(String token) {

        return getClaims(token).getIssuedAt().getTime();
    }

    // 클래임에서 만료 시간(Expiration)을 추출하는 메소드
    public long getExpiration(String token) {

        return getClaims(token).getExpiration().getTime();
    }

    // 토큰이 유효한지 확인하는 메소드 (토큰이 유효하면 true, 만료되었으면 false 반환)
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.debug("[JwtUtil] token validation failed: {}", e.getMessage());
            return false;
        }
    }


    // JWT(JSON Web Token)에서 클래임을 추출하는 메소드
    private Claims getClaims(String token) {
        // 토큰이 만료되면 parseSignedClaims() 메소드에서
        // ExpiredJwtException 예외가 발생하여 코드가 실행되지 않기 때문에 아래와 같이 예외 처리를 한다.
        try {
            return Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (Exception e) {
            log.error("[JwtUtil] failed to parse claims: {}", e.getMessage(), e);
            throw e;
        }
    }
}
