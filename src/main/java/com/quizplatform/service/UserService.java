package com.Quizvera.service;

import com.Quizvera.model.Role;
import com.Quizvera.model.User;
import com.Quizvera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public static class RegistrationException extends RuntimeException {
        public RegistrationException(String message) { super(message); }
    }

    public User registerStudent(String username, String rawPassword, String email, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new RegistrationException("Username is already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RegistrationException("An account with this email already exists.");
        }
        User user = new User(username, passwordEncoder.encode(rawPassword), email, fullName, Role.STUDENT);
        return userRepository.save(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public long countStudents() {
        return userRepository.countByRole(Role.STUDENT);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    public List<User> listStudents() {
        return userRepository.findByRoleOrderByFullNameAsc(Role.STUDENT).stream()
                .filter(u -> !u.isDeleted())
                .toList();
    }

    public List<User> searchStudents(String query) {
        if (query == null || query.isBlank()) {
            return listStudents();
        }
        String needle = query.trim().toLowerCase();
        return listStudents().stream()
                .filter(u -> u.getFullName().toLowerCase().contains(needle)
                        || u.getUsername().toLowerCase().contains(needle)
                        || u.getEmail().toLowerCase().contains(needle))
                .toList();
    }

    public User toggleEnabled(Long id) {
        User user = getById(id);
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Admin accounts cannot be deactivated.");
        }
        user.setEnabled(!user.isEnabled());
        return userRepository.save(user);
    }

    public void deleteAccount(Long id) {
        User user = getById(id);
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Admin accounts cannot be deleted.");
        }
        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);
    }
}