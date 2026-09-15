package com.queueapi.common.config;

import com.queueapi.entity.AdminEntity;
import com.queueapi.enums.AdminRole;
import com.queueapi.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-username}")
    private String defaultUsername;

    @Value("${app.admin.default-password}")
    private String defaultPassword;

    @Override
    public void run(String... args) {
        if (adminRepository.count() == 0) {
            AdminEntity admin = AdminEntity.builder()
                    .username(defaultUsername)
                    .password(passwordEncoder.encode(defaultPassword))
                    .role(AdminRole.ADMIN)
                    .build();
            adminRepository.save(admin);
            log.info("Seeded default admin user '{}'. Change the password in production.", defaultUsername);
        }
    }
}