package com.track.subscription_service.user.entity;

import jakarta.persistence.*;

import java.time.LocalTime;

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
    private Boolean emailNotificationsEnabled;
    private int reminderDaysBefore;

    @Column(nullable = false)
    private LocalTime reminderTime = LocalTime.of(9, 0);

    public int getReminderDaysBefore() {
        return reminderDaysBefore;
    }

    public void setReminderDaysBefore(int reminderDaysBefore) {
        this.reminderDaysBefore = reminderDaysBefore;
    }

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

    public UserPreferences(User user, String currency, String language, String timezone, String theme,Boolean emailNotificationsEnabled,int reminderDaysBefore) {
        this.user = user;
        this.currency = currency;
        this.language = language;
        this.timezone = timezone;
        this.theme = theme;
        this.emailNotificationsEnabled = emailNotificationsEnabled;
        this.reminderDaysBefore = reminderDaysBefore;
        this.reminderTime = LocalTime.of(9, 0);
    }

    public UserPreferences() {
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

    public Boolean getEmailNotificationsEnabled() {
        return emailNotificationsEnabled;
    }

    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) {
        this.emailNotificationsEnabled = emailNotificationsEnabled;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(LocalTime reminderTime) {
        this.reminderTime = reminderTime;
    }
}
