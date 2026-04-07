package com.track.subscription_service.subscription.entity;

import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String duration;
    private Double cost;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Subscription(){}
    public Subscription(String name, String type, String duration, Double cost) {
        this.name = name;
        this.type = type;
        this.duration = duration;
        this.cost = cost;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }



}
