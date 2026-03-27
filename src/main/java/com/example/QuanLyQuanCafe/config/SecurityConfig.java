package com.example.QuanLyQuanCafe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
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
                "/",
                "/login",
                "/register",
                "/error"
            ).permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/dashboard", true)
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
    public UserDetailsService userDetailsService() {
    // Tài khoản demo: admin / 123456
    UserDetails admin = User.withDefaultPasswordEncoder()
        .username("admin")
        .password("123456")
        .roles("ADMIN")
        .build();

    return new InMemoryUserDetailsManager(admin);
    }
}
