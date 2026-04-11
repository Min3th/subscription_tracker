package com.track.subscription_service.subscription.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository repo;

    public SubscriptionService(SubscriptionRepository repo) {
        this.repo = repo;
    }

    public List<Subscription> getAll(){
        return repo.findAll();
    }

    public Subscription create(Subscription subscription){
        return repo.save(subscription);
    }

    public List<Subscription> getByGoogleId(String googleId){
        return repo.findByUser_GoogleId(googleId);
    }
}
