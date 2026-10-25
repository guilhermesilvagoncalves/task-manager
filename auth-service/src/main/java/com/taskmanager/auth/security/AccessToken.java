package com.taskmanager.auth.security;

public record AccessToken(String value, long expiresIn) {
}
