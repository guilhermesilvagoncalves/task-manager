package com.taskmanager.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
