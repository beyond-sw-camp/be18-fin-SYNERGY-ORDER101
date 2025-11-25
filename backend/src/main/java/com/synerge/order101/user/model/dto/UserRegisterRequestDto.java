package com.synerge.order101.user.model.dto;

import com.synerge.order101.user.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisterRequestDto {

    private String email;

    private String password;

    private String name;

    private Role role;

    private String phone;

    private Long storeId;
}
