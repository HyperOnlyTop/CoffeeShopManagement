package com.example.QuanLyQuanCafe.controller;

import java.security.Principal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import com.example.QuanLyQuanCafe.model.AppUser;
import com.example.QuanLyQuanCafe.repository.AppUserRepository;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final AppUserRepository appUserRepository;

    public GlobalControllerAdvice(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @ModelAttribute("currentUser")
    public AppUser getCurrentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return appUserRepository.findByUsername(principal.getName());
    }
}
