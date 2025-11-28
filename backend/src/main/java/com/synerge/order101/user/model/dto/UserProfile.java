package com.synerge.order101.user.model.dto;

import com.synerge.order101.user.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    private Long userId;
    private String email;
    private String name;
    private Role role;
    private Boolean isActive;
    private String phone;
    private LocalDateTime createdAt;
}
