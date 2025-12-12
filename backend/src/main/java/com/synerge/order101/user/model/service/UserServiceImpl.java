package com.synerge.order101.user.model.service;

import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.user.exception.UserErrorCode;
import com.synerge.order101.user.model.dto.UserProfile;
import com.synerge.order101.user.model.dto.UserRegisterRequestDto;
import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import com.synerge.order101.store.model.entity.Store;
import com.synerge.order101.store.model.repository.StoreRepository;
import com.synerge.order101.store.exception.StoreErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public User registerUser(UserRegisterRequestDto userRequestDto) {
        String email = userRequestDto.getEmail().trim();
        if (checkEmailExists(email)) {
            throw new CustomException(UserErrorCode.DUPLICATE_EMAIL);
        }

        String rawPassword = userRequestDto.getPassword() == null ? "" : userRequestDto.getPassword().trim();
        String encoded = passwordEncoder.encode(rawPassword);

        Role role = userRequestDto.getRole() == null ? Role.STORE_ADMIN : userRequestDto.getRole();
        // 가맹점 관리자일 경우 storeId가 있으면 매핑
        Store store = null;
        if (role == Role.STORE_ADMIN && userRequestDto.getStoreId() != null) {
            Long storeId = userRequestDto.getStoreId();
            store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new CustomException(StoreErrorCode.STORE_NOT_FOUND));
        }

        User user = User.create(email, encoded, userRequestDto.getName(), role, userRequestDto.getPhone(), store);

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // DB 제약조건 위반(예: unique key duplicate) 처리
            throw new CustomException(UserErrorCode.DUPLICATE_STORE_ID);
    }}

    @Override
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getUserProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        return UserProfile.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .isActive(user.isActive())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkEmailExists(String email) {
        if (email == null) return false;
        return userRepository.findByEmail(email.trim()).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfile> findUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> UserProfile.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole())
                        .isActive(user.isActive())
                        .phone(user.getPhone())
                        .createdAt(user.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional
    public UserProfile toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        user.toggleActive();
        User saved = userRepository.save(user);

        return UserProfile.builder()
                .userId(saved.getUserId())
                .email(saved.getEmail())
                .name(saved.getName())
                .role(saved.getRole())
                .isActive(saved.isActive())
                .phone(saved.getPhone())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
