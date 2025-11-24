package com.synerge.order101.auth.model.dto;


import com.synerge.order101.user.model.entity.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@Builder
@RequiredArgsConstructor
public class LoginResponse {
    private final String accessToken;

    private final Long userId;

    private final String name;

    private final String phone;

    private final Role role;

    private final String type;

    private final long issuedAt;

    private final long expiresAt;

}
