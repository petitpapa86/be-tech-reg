package com.bcbs239.regtech.iam.presentation.bankprofile;

import com.bcbs239.regtech.core.presentation.routing.RouterAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RequestPredicates.contentType;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Router configuration for Bank Profile endpoints.
 * Defines URL mappings, permissions, and documentation tags.
 */
@Configuration
public class BankProfileRoutes {

    private final BankProfileController controller;

    public BankProfileRoutes(BankProfileController controller) {
        this.controller = controller;
    }

    @Bean
    public RouterFunction<ServerResponse> bankProfileRoutesConfig() {
        return RouterAttributes.withPermissions(
                route()
                    .GET("/api/v1/configuration/bank-profile/{bankId}", 
                            controller::getBankProfile)
                    .PUT("/api/v1/configuration/bank-profile",
                            controller::updateBankProfile)
                    .POST("/api/v1/configuration/bank-profile/{bankId}/reset",
                            controller::resetBankProfile)
                    .build(),
                "bank-profile:write"
            );
    }
}
