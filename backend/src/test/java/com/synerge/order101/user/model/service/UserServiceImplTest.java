package com.synerge.order101.user.model.service;

import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.repository.StoreRepository;
import com.synerge.order101.user.model.dto.UserProfile;
import com.synerge.order101.user.model.dto.UserRegisterRequestDto;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
@DisplayName("UserServiceImplTest")
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StoreRepository storeRepository;

    // ========================
    // registerUser
    // ========================
    @Test
    @Order(1)
    @DisplayName("사용자 등록 성공 - HQ_ADMIN")
    void registerUser_HqAdmin_Success() {
        // given
        UserRegisterRequestDto requestDto = UserRegisterRequestDto.builder()
                .email("admin@test.com")
                .password("password123")
                .name("관리자")
                .phone("010-1234-5678")
                .role(Role.HQ_ADMIN)
                .build();

        given(userRepository.findByEmail("admin@test.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");

        User savedUser = mock(User.class);
        given(savedUser.getEmail()).willReturn("admin@test.com");
        given(savedUser.getRole()).willReturn(Role.HQ_ADMIN);

        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        User result = userService.registerUser(requestDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("admin@test.com");
        assertThat(result.getRole()).isEqualTo(Role.HQ_ADMIN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @Order(2)
    @DisplayName("사용자 등록 성공 - STORE_ADMIN with Store")
    void registerUser_StoreAdmin_Success() {
        // given
        UserRegisterRequestDto requestDto = UserRegisterRequestDto.builder()
                .email("store@test.com")
                .password("password123")
                .name("가맹점주")
                .phone("010-9876-5432")
                .role(Role.STORE_ADMIN)
                .storeId(1L)
                .build();

        Store store = mock(Store.class);

        given(userRepository.findByEmail("store@test.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        User savedUser = mock(User.class);
        given(savedUser.getEmail()).willReturn("store@test.com");
        given(savedUser.getRole()).willReturn(Role.STORE_ADMIN);
        given(savedUser.getStore()).willReturn(store);

        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        User result = userService.registerUser(requestDto);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("store@test.com");
        assertThat(result.getRole()).isEqualTo(Role.STORE_ADMIN);
        assertThat(result.getStore()).isNotNull();
        verify(storeRepository).findById(1L);
    }

    @Test
    @Order(3)
    @DisplayName("사용자 등록 실패 - 이메일 중복")
    void registerUser_DuplicateEmail_Fail() {
        // given
        UserRegisterRequestDto requestDto = UserRegisterRequestDto.builder()
                .email("existing@test.com")
                .password("password123")
                .name("중복사용자")
                .build();

        User existingUser = mock(User.class);
        given(userRepository.findByEmail("existing@test.com")).willReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> userService.registerUser(requestDto))
                .isInstanceOf(CustomException.class);
    }

    // ========================
    // getUserByEmail
    // ========================
    @Test
    @Order(4)
    @DisplayName("이메일로 사용자 조회 성공")
    void getUserByEmail_Success() {
        // given
        String email = "test@test.com";
        User user = mock(User.class);
        given(user.getEmail()).willReturn(email);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        User result = userService.getUserByEmail(email);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    @Order(5)
    @DisplayName("이메일로 사용자 조회 실패 - 존재하지 않음")
    void getUserByEmail_NotFound() {
        // given
        String email = "notfound@test.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserByEmail(email))
                .isInstanceOf(CustomException.class);
    }

    // ========================
    // getUserProfileById
    // ========================
    @Test
    @Order(6)
    @DisplayName("사용자 프로필 조회 성공")
    void getUserProfileById_Success() {
        // given
        Long userId = 1L;
        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(user.getEmail()).willReturn("test@test.com");
        given(user.getName()).willReturn("테스트");
        given(user.getRole()).willReturn(Role.HQ_ADMIN);
        given(user.isActive()).willReturn(true);
        given(user.getPhone()).willReturn("010-1234-5678");
        given(user.getCreatedAt()).willReturn(LocalDateTime.now());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserProfile result = userService.getUserProfileById(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getName()).isEqualTo("테스트");
    }

    @Test
    @Order(7)
    @DisplayName("사용자 프로필 조회 실패 - 존재하지 않음")
    void getUserProfileById_NotFound() {
        // given
        Long userId = 999L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserProfileById(userId))
                .isInstanceOf(CustomException.class);
    }

    // ========================
    // checkEmailExists
    // ========================
    @Test
    @Order(8)
    @DisplayName("이메일 존재 여부 확인 - 존재함")
    void checkEmailExists_Exists() {
        // given
        String email = "existing@test.com";
        User user = mock(User.class);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        boolean result = userService.checkEmailExists(email);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("이메일 존재 여부 확인 - 존재하지 않음")
    void checkEmailExists_NotExists() {
        // given
        String email = "new@test.com";
        given(userRepository.findByEmail(email)).willReturn(Optional.empty());

        // when
        boolean result = userService.checkEmailExists(email);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @Order(10)
    @DisplayName("이메일 존재 여부 확인 - null 입력")
    void checkEmailExists_NullEmail() {
        // when
        boolean result = userService.checkEmailExists(null);

        // then
        assertThat(result).isFalse();
    }

    // ========================
    // findUsers
    // ========================
    @Test
    @Order(11)
    @DisplayName("사용자 목록 조회 성공")
    void findUsers_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        User user = mock(User.class);
        given(user.getUserId()).willReturn(1L);
        given(user.getEmail()).willReturn("test@test.com");
        given(user.getName()).willReturn("테스트");
        given(user.getRole()).willReturn(Role.HQ_ADMIN);
        given(user.isActive()).willReturn(true);
        given(user.getPhone()).willReturn("010-1234-5678");
        given(user.getCreatedAt()).willReturn(LocalDateTime.now());

        Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
        given(userRepository.findAll(pageable)).willReturn(userPage);

        // when
        Page<UserProfile> result = userService.findUsers(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("test@test.com");
    }

    // ========================
    // toggleUserActive
    // ========================
    @Test
    @Order(12)
    @DisplayName("사용자 활성화 상태 토글 성공")
    void toggleUserActive_Success() {
        // given
        Long userId = 1L;
        User user = mock(User.class);
        given(user.getUserId()).willReturn(userId);
        given(user.getEmail()).willReturn("test@test.com");
        given(user.getName()).willReturn("테스트");
        given(user.getRole()).willReturn(Role.HQ_ADMIN);
        given(user.isActive()).willReturn(false);  // toggled
        given(user.getPhone()).willReturn("010-1234-5678");
        given(user.getCreatedAt()).willReturn(LocalDateTime.now());

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userRepository.save(user)).willReturn(user);

        // when
        UserProfile result = userService.toggleUserActive(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isFalse();
        verify(user).toggleActive();
        verify(userRepository).save(user);
    }
}
