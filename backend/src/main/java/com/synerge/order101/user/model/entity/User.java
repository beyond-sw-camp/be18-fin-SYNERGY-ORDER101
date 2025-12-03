package com.synerge.order101.user.model.entity;

import com.synerge.order101.store.model.entity.Store;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "email", nullable = false, length = 50)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    // 개발/테스트 용도: 비밀번호 해시 업데이트 메서드
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // Add: update profile fields
    public void updateProfile(String name, String phone) {
        if (name != null && !name.isBlank()) this.name = name.trim();
        if (phone != null && !phone.isBlank()) this.phone = phone.trim();
    }

    // Add: soft delete
    public void softDelete() {
        this.isDeleted = true;
        this.isActive = false;
    }

    // Add: factory method to create a new User instance with required fields
    public static User create(String email, String encodedPassword, String name, Role role, String phone, Store store) {
        return new User(null, store, email, encodedPassword, name, role, true, null, null, false, phone);
    }

    // Add: toggle active status
    public void toggleActive() {
        this.isActive = !this.isActive;
    }

}
