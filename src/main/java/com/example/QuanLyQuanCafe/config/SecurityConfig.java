package com.example.QuanLyQuanCafe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                "/uploads/**",
                "/",
                "/menu",
                "/login",
                "/register",
                "/booking",
                "/reviews",
                "/error"
            ).permitAll()
            .requestMatchers(HttpMethod.GET, "/api/menu/**").permitAll()
            .requestMatchers("/api/chat/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/loyalty/**").hasRole("ADMIN")
            .requestMatchers("/api/bookings/**").hasAnyRole("ADMIN", "CASHIER", "SERVER")
            .requestMatchers(HttpMethod.POST, "/api/menu/**").hasRole("ADMIN")
            .requestMatchers("/Booking/new", "/Booking/edit/**", "/Booking/delete/**", "/Booking/save").hasRole("ADMIN")
            .requestMatchers("/dashboard/**").hasRole("ADMIN")
            .requestMatchers("/Menu/**", "/Revenue/**", "/Inventory/**", "/Setting/**", "/Accounts/**", "/Customers/**").hasRole("ADMIN")
            .requestMatchers("/Order/**").hasAnyRole("ADMIN", "CASHIER", "SERVER", "BARISTA")
            .requestMatchers("/Booking/**").hasAnyRole("ADMIN", "CASHIER", "SERVER")
            .requestMatchers("/Tables/**").hasAnyRole("ADMIN", "CASHIER", "SERVER")
            .requestMatchers("/Staff/**").hasAnyRole("ADMIN", "CASHIER", "SERVER", "BARISTA", "SECURITY")
            // Staff APIs
            .requestMatchers(HttpMethod.GET, "/api/staff").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/staff/basic").hasAnyRole("ADMIN", "CASHIER", "SERVER", "BARISTA", "SECURITY")
            .requestMatchers(HttpMethod.POST, "/api/staff").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/staff/update").hasRole("ADMIN")
            .requestMatchers("/api/staff/me").hasAnyRole("CASHIER", "SERVER", "BARISTA", "SECURITY")
            .requestMatchers("/api/staff/me/**").hasAnyRole("CASHIER", "SERVER", "BARISTA", "SECURITY")
            .requestMatchers("/api/staff/**").hasRole("ADMIN")
            .requestMatchers("/api/attendance/**").hasAnyRole("CASHIER", "SERVER", "BARISTA", "SECURITY")
            // Staff shifts: ai cũng xem được, chỉ ADMIN sửa
            .requestMatchers(HttpMethod.GET, "/api/staff-shifts/**").hasAnyRole("ADMIN", "CASHIER", "SERVER", "BARISTA", "SECURITY")
            .requestMatchers("/api/staff-shifts/**").hasRole("ADMIN")
            .requestMatchers("/api/tables/**").hasAnyRole("ADMIN", "CASHIER", "SERVER")
            // Orders API: tách quyền theo method
            .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("ADMIN", "CASHIER", "SERVER", "BARISTA")
            .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("ADMIN", "CASHIER")
            .requestMatchers(HttpMethod.PUT, "/api/orders/*/status").hasAnyRole("ADMIN", "CASHIER", "BARISTA")
            .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasAnyRole("ADMIN", "CASHIER")
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")
            .successHandler((request, response, authentication) -> {
                boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                boolean isCashier = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_CASHIER"));
                boolean isServer = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_SERVER"));
                boolean isBarista = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_BARISTA"));
                boolean isSecurity = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_SECURITY"));

                if (isAdmin) {
                    response.sendRedirect("/dashboard");
                    return;
                }
                if (isCashier || isServer || isBarista) {
                    response.sendRedirect("/Order");
                    return;
                }
                if (isSecurity) {
                    response.sendRedirect("/Staff");
                    return;
                }
                response.sendRedirect("/");
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
 