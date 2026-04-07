package com.track.subscription_service.user.entity;

import com.track.subscription_service.subscription.entity.Subscription;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String googleId;

    @Column(nullable = false)
    private String email;

    private String name;

    // for future proofness.
    private String provider = "Google";

    private Long createdAt;
    private Long updatedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Subscription> subscriptions;

    public User(String googleId, String email, String name, String provider, Long createdAt, Long updatedAt) {
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public User(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void addSubscription(Subscription subscription){

        if (subscriptions == null){
            subscriptions = new ArrayList<>();
        }

        subscriptions.add(subscription);
        subscription.setUser(this);
    }
}
