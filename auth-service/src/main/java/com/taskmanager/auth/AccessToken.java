package com.taskmanager.auth;

public record AccessToken(String value, long expiresIn) {
}
