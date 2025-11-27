package com.synerge.order101.user.controller;

import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.user.exception.UserErrorCode;
import com.synerge.order101.user.model.dto.UserProfile;
import com.synerge.order101.user.model.dto.UserRegisterRequestDto;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.service.UserService;
import com.synerge.order101.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<BaseResponseDto<User>> registerUser(@RequestBody  UserRegisterRequestDto userRequestDto) {

        User registeredUser = userService.registerUser(userRequestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BaseResponseDto<>(HttpStatus.CREATED, registeredUser));
    }


    @GetMapping("/me")
    public ResponseEntity<BaseResponseDto<User>> getMe(@AuthenticationPrincipal User authenticationUser) {
        if (authenticationUser == null) throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        User user = userService.getUserByEmail(authenticationUser.getEmail());

        // user 정보를 리턴한다.
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponseDto<UserProfile>> getUserById(@PathVariable Long userId) {
        UserProfile profile = userService.getUserProfileById(userId);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, profile));
    }

    @GetMapping("/check-id")
    public ResponseEntity<BaseResponseDto<Map<String, Object>>> getCheckId(

            @RequestParam  String email) {

        boolean isEmailExist = userService.checkEmailExists(email);

        Map<String, Object> data = new HashMap<>();

        if (isEmailExist) {
            throw new CustomException(UserErrorCode.DUPLICATE_EMAIL);
        } else {
            data.put("available", true);
        }

        return ResponseEntity.status(HttpStatus.OK).body(new BaseResponseDto<>(HttpStatus.OK, data));
    }

    @GetMapping
    public ResponseEntity<Page<UserProfile>> findUsers(Pageable pageable) {
        Page<UserProfile> response = userService.findUsers(pageable);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/toggle-active")
    public ResponseEntity<BaseResponseDto<UserProfile>> toggleUserActive(@PathVariable Long userId) {
        UserProfile updated = userService.toggleUserActive(userId);
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, updated));
    }
}
