package com.synerge.order101.auth.controller;

import com.synerge.order101.common.dto.BaseResponseDto;
import com.synerge.order101.common.exception.CustomException;
import com.synerge.order101.user.exception.UserErrorCode;
import com.synerge.order101.user.model.entity.User;
import com.synerge.order101.user.model.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/debug")
@RequiredArgsConstructor
public class DebugController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/user")
    public ResponseEntity<BaseResponseDto<DebugUserDto>> getUserByEmail(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        DebugUserDto dto = new DebugUserDto(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isActive(),
                user.isDeleted(),
                user.getPhone()
        );

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, dto));
    }

    @GetMapping("/password/check")
    public ResponseEntity<BaseResponseDto<Boolean>> checkPassword(
            @RequestParam String email,
            @RequestParam String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        boolean matches = passwordEncoder.matches(password == null ? "" : password.trim(), user.getPassword());

        if (matches) {
            return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, matches));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new BaseResponseDto<>(HttpStatus.UNAUTHORIZED, matches));
        }
    }

    @GetMapping("/password/hash")
    public ResponseEntity<BaseResponseDto<String>> getPasswordHash(@RequestParam String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        String hash = user.getPassword();
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, hash));
    }

    @GetMapping("/password/encode")
    public ResponseEntity<BaseResponseDto<String>> encodePassword(@RequestParam String password) {
        // 개발용: 입력된 평문을 BCrypt로 인코딩하여 반환
        String encoded = passwordEncoder.encode(password == null ? "" : password.trim());
        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, encoded));
    }

    @PostMapping("/password/update")
    public ResponseEntity<BaseResponseDto<Boolean>> updatePassword(
            @RequestParam String email,
            @RequestParam String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        String encoded = passwordEncoder.encode(password == null ? "" : password.trim());
        user.changePassword(encoded);
        userRepository.save(user);

        return ResponseEntity.ok(new BaseResponseDto<>(HttpStatus.OK, true));
    }
}
