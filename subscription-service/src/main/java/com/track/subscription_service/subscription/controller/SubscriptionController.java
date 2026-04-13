package com.track.subscription_service.subscription.controller;

import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.subscription.service.SubscriptionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service){
        this.service = service;
    }

    @GetMapping
    public List<Subscription> getUserSubscriptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();

        return service.getByGoogleId(googleId);

    }

    @GetMapping("/{id}")
    public Subscription getSubscription(
            @PathVariable Long id,
            Authentication auth
    ){
        return service.getByIdAndGoogleId(id, auth.getName());
    }

    @PostMapping
    public Subscription createSubscription(
            @RequestBody Subscription subscription){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();
        return service.create(subscription,googleId);
    }

    @PutMapping("/{id}")
    public Subscription updateSubscription(
            @PathVariable Long id,
            @RequestBody Subscription updated
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();

        return service.update(id,updated,googleId);
    }

    @DeleteMapping("/{id}")
    public void deleteSubscription(
            @PathVariable Long id
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();
        service.delete(id,googleId);
    }
}
