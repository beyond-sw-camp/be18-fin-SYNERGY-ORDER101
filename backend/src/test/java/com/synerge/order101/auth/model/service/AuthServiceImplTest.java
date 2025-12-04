package com.synerge.order101.auth.model.service;

import com.synerge.order101.auth.jwt.JwtTokenProvider;
import com.synerge.order101.auth.jwt.JwtUtil;
import com.synerge.order101.auth.model.dto.LoginResponse;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("AuthServiceImplTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    // ========================
    // login
    // ========================
    @Test
    @Order(1)
    @DisplayName("로그인 성공 - Store 없는 사용자")
    void login_Success_WithoutStore() {
        // given
        String email = "admin@test.com";
        String password = "password123";
        String accessToken = "test.access.token";

        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);
        given(user.getPassword()).willReturn("encodedPassword");
        given(user.getRole()).willReturn(Role.HQ_ADMIN);
        given(user.getName()).willReturn("관리자");
        given(user.getPhone()).willReturn("010-1234-5678");
        given(user.getStore()).willReturn(null);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, "encodedPassword")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(1L, Role.HQ_ADMIN)).willReturn(accessToken);
        given(jwtUtil.getIssuedAt(accessToken)).willReturn(System.currentTimeMillis());
        given(jwtUtil.getExpiration(accessToken)).willReturn(System.currentTimeMillis() + 3600000L);

        // when
        LoginResponse result = authService.login(email, password);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(accessToken);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(Role.HQ_ADMIN);
        assertThat(result.getStoreId()).isNull();
        verify(userRepository).findByEmail(email);
    }

    @Test
    @Order(2)
    @DisplayName("로그인 성공 - Store 있는 사용자")
    void login_Success_WithStore() {
        // given
        String email = "store@test.com";
        String password = "password123";
        String accessToken = "test.access.token";

        Store store = mock(Store.class);
        given(store.getStoreId()).willReturn(10L);

        User user = mock(User.class);
        given(user.getUserId()).willReturn(2L);
        given(user.getPassword()).willReturn("encodedPassword");
        given(user.getRole()).willReturn(Role.STORE_ADMIN);
        given(user.getName()).willReturn("가맹점주");
        given(user.getPhone()).willReturn("010-9876-5432");
        given(user.getStore()).willReturn(store);

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, "encodedPassword")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(2L, Role.STORE_ADMIN)).willReturn(accessToken);
        given(jwtUtil.getIssuedAt(accessToken)).willReturn(System.currentTimeMillis());
        given(jwtUtil.getExpiration(accessToken)).willReturn(System.currentTimeMillis() + 3600000L);

        // when
        LoginResponse result = authService.login(email, password);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(2L);
        assertThat(result.getRole()).isEqualTo(Role.STORE_ADMIN);
        assertThat(result.getStoreId()).isEqualTo(10L);
    }

    @Test
    @Order(3)
    @DisplayName("로그인 실패 - 사용자 없음")
    void login_UserNotFound() {
        // given
        String email = "notfound@test.com";
        String password = "password123";

        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(email, password))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @Order(4)
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_InvalidPassword() {
        // given
        String email = "admin@test.com";
        String password = "wrongPassword";

        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);
        given(user.getPassword()).willReturn("encodedPassword");

        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(password, "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(email, password))
                .isInstanceOf(CustomException.class);
    }

    // ========================
    // createRefreshToken
    // ========================
    @Test
    @Order(5)
    @DisplayName("리프레시 토큰 생성 성공")
    void createRefreshToken_Success() {
        // given
        Long userId = 1L;
        String refreshToken = "test.refresh.token";
        given(jwtTokenProvider.createRefreshToken(userId)).willReturn(refreshToken);

        // when
        String result = authService.createRefreshToken(userId);

        // then
        assertThat(result).isEqualTo(refreshToken);
        verify(jwtTokenProvider).createRefreshToken(userId);
    }

    // ========================
    // logout
    // ========================
    @Test
    @Order(6)
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        String bearerToken = "Bearer test.access.token";
        String accessToken = "test.access.token";

        given(jwtTokenProvider.resolveToken(bearerToken)).willReturn(accessToken);

        // when
        authService.logout(bearerToken);

        // then
        verify(jwtTokenProvider).addBlacklist(accessToken);
        verify(jwtTokenProvider).deleteRefreshToken(accessToken);
    }

    @Test
    @Order(7)
    @DisplayName("로그아웃 실패 - 토큰 없음")
    void logout_NoToken() {
        // given
        String bearerToken = "InvalidToken";
        given(jwtTokenProvider.resolveToken(bearerToken)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> authService.logout(bearerToken))
                .isInstanceOf(CustomException.class);
    }

    // ========================
    // refreshAccessToken
    // ========================
    @Test
    @Order(8)
    @DisplayName("액세스 토큰 갱신 성공")
    void refreshAccessToken_Success() {
        // given
        String refreshToken = "test.refresh.token";
        String newAccessToken = "new.access.token";

        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);
        given(user.getRole()).willReturn(Role.HQ_ADMIN);
        given(user.getName()).willReturn("관리자");
        given(user.getPhone()).willReturn("010-1234-5678");
        given(user.getStore()).willReturn(null);

        given(jwtUtil.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isValidRefreshToken(refreshToken)).willReturn(true);
        given(jwtUtil.getUserId(refreshToken)).willReturn("1");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(1L, Role.HQ_ADMIN)).willReturn(newAccessToken);
        given(jwtUtil.getIssuedAt(newAccessToken)).willReturn(System.currentTimeMillis());
        given(jwtUtil.getExpiration(newAccessToken)).willReturn(System.currentTimeMillis() + 3600000L);

        // when
        LoginResponse result = authService.refreshAccessToken(refreshToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(newAccessToken);
    }

    @Test
    @Order(9)
    @DisplayName("액세스 토큰 갱신 실패 - 유효하지 않은 리프레시 토큰")
    void refreshAccessToken_InvalidToken() {
        // given
        String refreshToken = "invalid.refresh.token";
        given(jwtUtil.validateToken(refreshToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @Order(10)
    @DisplayName("액세스 토큰 갱신 실패 - Redis에 없는 토큰")
    void refreshAccessToken_NotInRedis() {
        // given
        String refreshToken = "test.refresh.token";
        given(jwtUtil.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.isValidRefreshToken(refreshToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @Order(11)
    @DisplayName("액세스 토큰 갱신 실패 - null 토큰")
    void refreshAccessToken_NullToken() {
        // when & then
        assertThatThrownBy(() -> authService.refreshAccessToken(null))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @Order(12)
    @DisplayName("액세스 토큰 갱신 실패 - 빈 토큰")
    void refreshAccessToken_BlankToken() {
        // when & then
        assertThatThrownBy(() -> authService.refreshAccessToken("   "))
                .isInstanceOf(CustomException.class);
    }
}
