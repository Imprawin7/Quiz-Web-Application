package com.quizplatform.config;

import com.quizplatform.model.Role;
import com.quizplatform.model.User;
import com.quizplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final org.springframework.core.env.Environment env;

    @Override
    public void run(String... args) {
        String adminUsername = env.getProperty("app.admin.username", "admin");
        String adminPassword = env.getProperty("app.admin.password", "Admin@123");
        String adminEmail = env.getProperty("app.admin.email", "admin@quizplatform.com");

        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User(
                    adminUsername,
                    passwordEncoder.encode(adminPassword),
                    adminEmail,
                    "Boss",
                    Role.ADMIN
            );
            userRepository.save(admin);
            System.out.println("==============================================");
            System.out.println("Default admin account created:");
            System.out.println("   username: " + adminUsername);
            System.out.println("   password: " + adminPassword);
            System.out.println("==============================================");
        }
    }
}
