package com.Quizvera.repository;

import com.Quizvera.model.Role;
import com.Quizvera.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    List<User> findByRoleOrderByFullNameAsc(Role role);
}