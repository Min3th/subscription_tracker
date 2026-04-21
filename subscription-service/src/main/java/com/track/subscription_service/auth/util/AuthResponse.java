package com.track.subscription_service.auth.util;

import com.track.subscription_service.user.entity.User;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private User user;


    public AuthResponse(String accessToken, String refreshToken, User user) {
        this.accessToken = accessToken;
        this.user = user;
        this.refreshToken = refreshToken;
    }

    public AuthResponse(String accessToken, User user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }



}
