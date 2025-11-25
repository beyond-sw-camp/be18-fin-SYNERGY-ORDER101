package com.synerge.order101.user.model.repository;

import com.synerge.order101.user.model.entity.Role;
import com.synerge.order101.user.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);
}
