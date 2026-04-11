package com.track.subscription_service.subscription.controller;

import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.subscription.service.SubscriptionService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;
    private JwtService jwtService;

    public SubscriptionController(SubscriptionService service){
        this.service = service;
    }

    @GetMapping
    public List<Subscription> getUserSubscriptions(@RequestHeader("Authorization") String authHeader){
        String token = authHeader.replace("Bearer ","");
        String googleId = jwtService.extractGoogleId(token);

        return service.getByGoogleId(googleId);

    }

    @PostMapping
    public Subscription createSubscription(@RequestBody Subscription subscription){
        return service.create(subscription);
    }
}
