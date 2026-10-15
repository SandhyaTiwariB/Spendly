package com.spendly.dto;

import java.math.BigDecimal;

public class AuthResponse {

    private boolean success;
    private String message;
    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private String currency;
    private BigDecimal monthlyBudget;

    public AuthResponse() {}

    public AuthResponse(boolean success, String message, String token, Long userId, String email,
                        String fullName, String role, String currency, BigDecimal monthlyBudget) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.currency = currency;
        this.monthlyBudget = monthlyBudget;
    }

    public static AuthResponse failure(String message) {
        AuthResponse res = new AuthResponse();
        res.setSuccess(false);
        res.setMessage(message);
        return res;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public java.util.Map<String, Object> getUser() {
        java.util.Map<String, Object> u = new java.util.HashMap<>();
        u.put("id", userId);
        u.put("email", email);
        u.put("fullName", fullName);
        u.put("role", role);
        u.put("currency", currency != null ? currency : "₹");
        u.put("monthlyBudget", monthlyBudget != null ? monthlyBudget : new BigDecimal("40000.00"));
        return u;
    }
}
