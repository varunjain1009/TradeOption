package com.tradeoption.config;

import com.tradeoption.domain.User;
import com.tradeoption.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@tradeoption.com");
                admin.setRole("ROLE_ADMIN");
                userRepository.save(admin);
                System.out.println("Default admin user created: admin / admin123");
            }
        };
    }
}
