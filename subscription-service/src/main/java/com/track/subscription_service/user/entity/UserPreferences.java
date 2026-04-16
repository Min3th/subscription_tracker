package com.track.subscription_service.user.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    @Id
    private Long userId;

    @OneToOne
    @JoinColumn(name="user_id")
    @MapsId
    private User user;

    private String currency;
    private String language;
    private String timezone;
    private String theme;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserPreferences(User user, String currency, String language, String timezone, String theme) {
        this.user = user;
        this.currency = currency;
        this.language = language;
        this.timezone = timezone;
        this.theme = theme;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }


}
