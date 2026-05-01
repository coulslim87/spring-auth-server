package com.example.authserver.controller;

import com.example.authserver.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetailsImpl principal, Model model) {
        model.addAttribute("user", principal.getUser());
        return "home";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}
