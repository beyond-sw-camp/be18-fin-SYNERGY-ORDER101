package com.synerge.order101.user.model.service;

import com.synerge.order101.user.model.dto.UserProfile;
import com.synerge.order101.user.model.dto.UserRegisterRequestDto;
import com.synerge.order101.user.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    User registerUser(UserRegisterRequestDto userRequestDto);

    User getUserByEmail(String email);

    UserProfile getUserProfileById(Long userId);

    boolean checkEmailExists(String email);

    // 모든 사용자 페이징 조회
    Page<UserProfile> findUsers(Pageable pageable);

    // 사용자 활성화 상태 토글
    UserProfile toggleUserActive(Long userId);
}
