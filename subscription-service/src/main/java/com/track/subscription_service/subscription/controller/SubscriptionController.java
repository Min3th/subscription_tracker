package com.track.subscription_service.subscription.controller;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionRepository repo;

    public SubscriptionController(SubscriptionRepository repo){
        this.repo = repo;
    }

    @GetMapping
    public List<Subscription> getAll(){
        return repo.findAll();
    }

    @PostMapping
    public Subscription createSubscription(@RequestBody Subscription subscription){
        return repo.save(subscription);
    }
}
