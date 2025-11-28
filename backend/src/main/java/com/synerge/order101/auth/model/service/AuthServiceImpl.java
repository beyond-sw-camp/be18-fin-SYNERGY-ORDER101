package com.synerge.order101.auth.model.service;


import com.synerge.order101.auth.exception.AuthErrorCode;
import com.synerge.order101.auth.jwt.JwtTokenProvider;
import com.synerge.order101.auth.jwt.JwtUtil;
import com.synerge.order101.auth.model.dto.LoginResponse;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.user.exception.UserErrorCode;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {


    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public LoginResponse login(String email, String password) {

        // 디버그: 전달된 이메일 확인
        log.debug("AuthService.login called with email={}", email);

        // 입력 비밀번호 정리
        String rawPassword = password == null ? "" : password.trim();

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            log.warn("Login failed - user not found for email={}", email);
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            log.warn("Login failed - invalid password for userId={}", user.getUserId());
            // 디버그: 제공된 평문 비밀번호(정리됨)와 DB의 해시 확인 (운영에서는 절대 노출 금지)
            log.debug("Provided password='{}', Stored hash='{}'", rawPassword, user.getPassword());
            throw new CustomException(AuthErrorCode.INVALID_PASSWORD);
        }

        return createLoginResponse(user);
    }

    private LoginResponse createLoginResponse(User user) {
        // AccessToken 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole());

        // Store가 null일 수 있으므로 안전하게 처리
        Long storeId = null;
        if (user.getStore() != null) {
            storeId = user.getStore().getStoreId();
        }

        return LoginResponse.builder()
                .accessToken(accessToken)
                .userId(user.getUserId())
                .type("Bearer")
                .name(user.getName())
                .phone(user.getPhone())
                .storeId(storeId)
                .issuedAt(jwtUtil.getIssuedAt(accessToken))
                .expiresAt(jwtUtil.getExpiration(accessToken))
                .role(user.getRole())
                .build();
    }

    @Override
    public String createRefreshToken(Long userId) {

        return jwtTokenProvider.createRefreshToken(userId);

    }

    @Override
    public void logout(String bearerToken) {
        String accessToken = jwtTokenProvider.resolveToken(bearerToken);

        if (accessToken == null) {
            throw new CustomException(AuthErrorCode.UNAUTHORIZED_TOKEN);
        }

        jwtTokenProvider.addBlacklist(accessToken);
        jwtTokenProvider.deleteRefreshToken(accessToken);
    }

    @Override
    public LoginResponse refreshAccessToken(String refreshToken) {
        // 1. 리프레시 토큰(Refresh Token) 검증
        if (refreshToken == null || refreshToken.isBlank() || !jwtUtil.validateToken(refreshToken)) {
            throw new CustomException(AuthErrorCode.UNAUTHORIZED_TOKEN);
        }

        // 2. Redis의 리프레시 토큰(Refresh Token)과 비교
        if (!jwtTokenProvider.isValidRefreshToken(refreshToken)) {
            throw new CustomException(AuthErrorCode.UNAUTHORIZED_TOKEN);
        }

        // 3. 사용자 정보 조회 후 새로운 LoginResponse 객체를 생성
        Long userId = Long.parseLong(jwtUtil.getUserId(refreshToken));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return createLoginResponse(user);
    }
}