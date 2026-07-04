package com.printease.backend;

import com.printease.backend.entity.User;
import com.printease.backend.entity.enums.Role;
import com.printease.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@Slf4j
public class PrintEaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrintEaseApplication.class, args);
    }

    /**
     * One-off production seed runner.
     * Only runs when SEED_SUPER_ADMIN=true is set as an environment variable.
     * Use on first boot of a fresh production database, then remove the env var.
     */
    @Bean
    public ApplicationRunner seedSuperAdmin(
            Environment env,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            String seedFlag = env.getProperty("SEED_SUPER_ADMIN", "false");
            if (!"true".equalsIgnoreCase(seedFlag)) {
                return;
            }

            String email = env.getProperty("SEED_SUPER_ADMIN_EMAIL", "superadmin@printease.com");
            String password = env.getProperty("SEED_SUPER_ADMIN_PASSWORD", "SuperAdmin123!");
            String name = env.getProperty("SEED_SUPER_ADMIN_NAME", "Super Admin");

            if (userRepository.existsByEmail(email)) {
                log.info("Super Admin user '{}' already exists — skipping seed.", email);
                return;
            }

            User superAdmin = User.builder()
                    .name(name)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(Role.SUPER_ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(superAdmin);

            log.info("✅ Seeded Super Admin user: {}", email);
        };
    }
}
