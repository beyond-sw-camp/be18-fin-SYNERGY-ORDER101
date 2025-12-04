package com.synerge.order101.auth.controller;

import java.time.Duration;

import com.synerge.order101.common.dto.BaseResponseDto;
import org.hibernate.annotations.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.synerge.order101.auth.model.dto.LoginRequestDto;
import com.synerge.order101.auth.model.dto.LoginResponse;
import com.synerge.order101.auth.model.service.AuthService;
import com.synerge.order101.auth.model.service.JwtCookieService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtCookieService jwtCookieService;

    @PostMapping("/login")
    public ResponseEntity<BaseResponseDto<LoginResponse>> login(
             @RequestBody LoginRequestDto loginRequestDto) {

        // login에 필요한 정보를 가져온다.
        // 사용자 확인
        LoginResponse loginResponse = authService.login(
                loginRequestDto.getEmail(),
                loginRequestDto.getPassword());
        // 사용자 확인 후 이상없다면 토큰 발행.
        String refreshToken = authService.createRefreshToken(loginResponse.getUserId());
        ResponseCookie cookie = jwtCookieService.createRefreshTokenCookie(refreshToken, Duration.ofDays(1));
        HttpHeaders headers = jwtCookieService.createRefreshTokenCookieHeaders(cookie);

        return ResponseEntity.ok().headers(headers).body(new BaseResponseDto<>(HttpStatus.OK,loginResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
@RequestHeader("Authorization") String bearerToken) {

        authService.logout(bearerToken);

        ResponseCookie cookie = jwtCookieService.deleteRefreshTokenCookie();
        HttpHeaders headers = jwtCookieService.createRefreshTokenCookieHeaders(cookie);

        return ResponseEntity
                .noContent()
                .headers(headers)
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<BaseResponseDto<LoginResponse>> refresh(
 @CookieValue(name = "refresh_token", defaultValue = "") String refreshToken) {

        LoginResponse loginResponse = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, loginResponse));
    }

}
