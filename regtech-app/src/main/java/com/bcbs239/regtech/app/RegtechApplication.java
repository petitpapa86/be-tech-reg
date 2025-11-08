package com.bcbs239.regtech.app;

import com.bcbs239.regtech.iam.application.users.RegisterUserCommandHandler;
import com.bcbs239.regtech.iam.infrastructure.database.repositories.JpaUserRepository;
import com.bcbs239.regtech.iam.presentation.users.UserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.function.RouterFunction;

import java.util.Arrays;

/**
 * Main application class for the RegTech modular monolith.
 * This module orchestrates all domain modules (IAM, Billing, etc.)
 * and provides centralized configuration and startup.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.bcbs239.regtech.app",
        "com.bcbs239.regtech.core",
        "com.bcbs239.regtech.iam.infrastructure.config"
})
@EntityScan(basePackages = {
        "com.bcbs239.regtech.core.infrastructure",
})
@EnableJpaRepositories(basePackages = {
        "com.bcbs239.regtech.core.infrastructure",
})
@EnableAspectJAutoProxy
public class RegtechApplication {
    static void main(String[] args) {
        SpringApplication.run(RegtechApplication.class, args);
    }

}

