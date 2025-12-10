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

        // 계정 활성화 여부 확인
        if (!user.isActive()) {
            log.warn("Login failed - account disabled for userId={}", user.getUserId());
            throw new CustomException(AuthErrorCode.ACCOUNT_DISABLED);
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
            throw new CustomException(AuthErrorCode.TOKEN_NOT_PROVIDED);
        }

        // 토큰 유효성 검사 및 구체적 에러 매핑
        try {
            // 만료 여부 확인
            long exp = jwtUtil.getExpiration(accessToken);
            if (exp < System.currentTimeMillis()) {
                throw new CustomException(AuthErrorCode.TOKEN_EXPIRED);
            }

            // validateToken이 false이면 형식/서명 문제가 의심됨
            if (!jwtUtil.validateToken(accessToken)) {
                throw new CustomException(AuthErrorCode.INVALID_TOKEN);
            }
        } catch (CustomException ce) {
            throw ce; // 이미 적절한 AuthErrorCode로 변환된 예외
        } catch (Exception e) {
            log.warn("Logout failed - malformed token: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.MALFORMED_TOKEN);
        }

        jwtTokenProvider.addBlacklist(accessToken);
        jwtTokenProvider.deleteRefreshToken(accessToken);
    }

    @Override
    public LoginResponse refreshAccessToken(String refreshToken) {
        // 1. 리프레시 토큰(Refresh Token) 미제공 검사
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(AuthErrorCode.TOKEN_NOT_PROVIDED);
        }

        // 2. 토큰 파싱/유효성 검사 - 만료/유효하지 않음/손상 구분
        try {
            long exp = jwtUtil.getExpiration(refreshToken);
            if (exp < System.currentTimeMillis()) {
                throw new CustomException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
            }

            if (!jwtUtil.validateToken(refreshToken)) {
                throw new CustomException(AuthErrorCode.INVALID_TOKEN);
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.warn("Refresh token validation failed - malformed token: {}", e.getMessage());
            throw new CustomException(AuthErrorCode.MALFORMED_TOKEN);
        }

        // 3. Redis의 리프레시 토큰(Refresh Token)과 비교
        if (!jwtTokenProvider.isValidRefreshToken(refreshToken)) {
            throw new CustomException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 4. 사용자 정보 조회 후 새로운 LoginResponse 객체를 생성
        Long userId = Long.parseLong(jwtUtil.getUserId(refreshToken));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 계정 활성화 여부 확인
        if (!user.isActive()) {
            log.warn("Refresh failed - account disabled for userId={}", user.getUserId());
            throw new CustomException(AuthErrorCode.ACCOUNT_DISABLED);
        }

        return createLoginResponse(user);
    }
}