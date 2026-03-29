package com.example.QuanLyQuanCafe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(
                "/css/**",
                "/js/**",
                "/images/**",
                "/static/**",
                "/upload/**",
                "/",
                "/login",
                "/register",
                "/error"
            ).permitAll()
            .requestMatchers("/api/chat/**").permitAll()
            .requestMatchers("/dashboard/**", "/Menu/**", "/Order/**", "/Revenue/**", "/Inventory/**", "/Setting/**").hasAnyRole("ADMIN", "STAFF")
            .requestMatchers("/Staff/**").hasAnyRole("ADMIN", "STAFF")
            .requestMatchers("/api/staff/me").hasRole("STAFF")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .successHandler((request, response, authentication) -> {
                boolean isAdminOrStaff = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
                if (isAdminOrStaff) {
                    response.sendRedirect("/dashboard");
                } else {
                    response.sendRedirect("/");
                }
            })
            .permitAll()
        )
        .logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
            .permitAll()
        )
        .httpBasic(basic -> basic.disable())
        .oauth2Login(oauth2 -> oauth2.disable());

    return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
