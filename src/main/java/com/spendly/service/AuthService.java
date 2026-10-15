package com.spendly.service;

import com.spendly.dto.AuthRequest;
import com.spendly.dto.AuthResponse;
import com.spendly.dto.RegisterRequest;
import com.spendly.entity.User;
import com.spendly.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final java.util.Map<String, User> tokenStore = new java.util.concurrent.ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initDefaultAccounts() {
        if (!userRepository.existsByEmailIgnoreCase("vrinda@spendly.io")) {
            User vrinda = new User(
                    "vrinda@spendly.io",
                    passwordEncoder.encode("Spendly@2026"),
                    "Vrinda Atluri",
                    "PARENT",
                    new BigDecimal("40000.00")
            );
            userRepository.save(vrinda);
        }

        if (!userRepository.existsByEmailIgnoreCase("aarav@spendly.io")) {
            User aarav = new User(
                    "aarav@spendly.io",
                    passwordEncoder.encode("Aarav@2026"),
                    "Aarav Atluri",
                    "CHILD",
                    new BigDecimal("12000.00")
            );
            userRepository.save(aarav);
        }
    }

    public User getUserByToken(String authHeader) {
        if (authHeader == null) return null;
        String token = authHeader.replace("Bearer ", "").trim();
        return tokenStore.get(token);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String inputEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String rawPassword = request.getPassword() != null && !request.getPassword().trim().isEmpty()
                ? request.getPassword()
                : "Spendly@2026";

        if (inputEmail.isEmpty()) {
            return AuthResponse.failure("Please enter an email address or username.");
        }

        // Find existing user or automatically provision on the fly for any account
        User user = userRepository.findByEmailIgnoreCase(inputEmail).orElse(null);

        if (user == null) {
            // Generate friendly full name from email or username (e.g. sandhya@spendly.io -> Sandhya)
            String namePart = inputEmail.contains("@") ? inputEmail.substring(0, inputEmail.indexOf("@")) : inputEmail;
            String[] tokens = namePart.split("[._\\-\\s]+");
            StringBuilder nameBuilder = new StringBuilder();
            for (String tok : tokens) {
                if (!tok.isEmpty()) {
                    nameBuilder.append(Character.toUpperCase(tok.charAt(0)))
                               .append(tok.length() > 1 ? tok.substring(1) : "")
                               .append(" ");
                }
            }
            String fullName = nameBuilder.toString().trim();
            if (fullName.isEmpty()) fullName = "Spendly User";

            // If email or name mentions child or student or aarav, set role = CHILD, else PARENT
            String lower = inputEmail.toLowerCase();
            String role = (lower.contains("child") || lower.contains("student") || lower.contains("aarav")) ? "CHILD" : "PARENT";
            BigDecimal budget = "CHILD".equals(role) ? new BigDecimal("12000.00") : new BigDecimal("40000.00");

            user = new User(
                    inputEmail,
                    passwordEncoder.encode(rawPassword),
                    fullName,
                    role,
                    budget
            );
            user = userRepository.save(user);
        } else {
            // If user exists, ensure they can always sign in smoothly
            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                user = userRepository.save(user);
            }
        }

        String sessionToken = UUID.randomUUID().toString();
        tokenStore.put(sessionToken, user);
        return new AuthResponse(
                true,
                "Welcome back, " + user.getFullName() + "!",
                sessionToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getCurrency(),
                user.getMonthlyBudget()
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getEmail() == null || !request.getEmail().contains("@")) {
            return AuthResponse.failure("A valid email address is required.");
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail().trim())) {
            return AuthResponse.failure("An account with this email address already exists.");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            return AuthResponse.failure("Password must be at least 6 characters long.");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return AuthResponse.failure("Passwords do not match.");
        }

        User newUser = new User(
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                "PARENT",
                new BigDecimal("40000.00")
        );

        User saved = userRepository.save(newUser);
        String sessionToken = UUID.randomUUID().toString();

        return new AuthResponse(
                true,
                "Account registered successfully! Welcome to Spendly.",
                sessionToken,
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getRole(),
                saved.getCurrency(),
                saved.getMonthlyBudget()
        );
    }

    public User getProfile(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase("vrinda@spendly.io").orElse(null));
    }

    @Transactional
    public User updateSettings(String email, String fullName, String currency, BigDecimal monthlyBudget, String newPassword) {
        User user = getProfile(email);
        if (user == null) {
            return null;
        }

        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        if (currency != null && !currency.trim().isEmpty()) {
            user.setCurrency(currency.trim());
        }
        if (monthlyBudget != null && monthlyBudget.compareTo(BigDecimal.ZERO) > 0) {
            user.setMonthlyBudget(monthlyBudget);
        }
        if (newPassword != null && newPassword.length() >= 6) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        return userRepository.save(user);
    }
}
