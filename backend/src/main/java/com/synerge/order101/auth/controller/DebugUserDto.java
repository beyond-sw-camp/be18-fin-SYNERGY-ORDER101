package com.synerge.order101.auth.controller;

import com.synerge.order101.user.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DebugUserDto {
    private Long userId;
    private String email;
    private String name;
    private Role role;
    private boolean isActive;
    private boolean isDeleted;
    private String phone;
}

