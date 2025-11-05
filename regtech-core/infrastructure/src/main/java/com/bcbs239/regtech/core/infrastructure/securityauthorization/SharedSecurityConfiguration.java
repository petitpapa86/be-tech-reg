package com.bcbs239.regtech.core.infrastructure.securityauthorization;

import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import org.springframework.security.web.SecurityFilterChain;

@Configuration
// @EnableWebSecurity - Disabled in favor of ModularSecurityConfiguration
public class SharedSecurityConfiguration {

    // Security configuration moved to ModularSecurityConfiguration
    // @Bean
    // public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    //     http
    //         .authorizeHttpRequests(authz -> authz
    //             .requestMatchers("/actuator/**").permitAll()
    //             .requestMatchers("/iam/**").hasRole("USER")
    //             .requestMatchers("/billing/**").hasRole("USER")
    //             .anyRequest().authenticated()
    //         )
    //         .httpBasic(httpBasic -> {});
    //
    //     return http.build();
    // }
}
