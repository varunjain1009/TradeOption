package com.tradeoption.controller;

import com.tradeoption.domain.User;
import com.tradeoption.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            @RequestParam(required = false) String email,
            Model model) {

        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }

        User user = new User(username, passwordEncoder.encode(password), email);
        userRepository.save(user);

        return "redirect:/login?registered";
    }
}
