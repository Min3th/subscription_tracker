package com.track.subscription_service.subscription.controller;

import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.subscription.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@CrossOrigin(origins = "https://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service){
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Subscription>>getUserSubscriptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();

        return ResponseEntity.ok(service.getByGoogleId(googleId));

    }

    @GetMapping("/{id}")
    public ResponseEntity<Subscription> getSubscription(
            @PathVariable Long id,
            Authentication auth
    ){
        return ResponseEntity.ok(service.getByIdAndGoogleId(id, auth.getName()));
    }

    @PostMapping
    public ResponseEntity<Subscription> createSubscription(
            @RequestBody Subscription subscription){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();
        Subscription created = service.create(subscription,googleId);
        URI location = URI.create("/subscriptions/" + created.getId());

        return ResponseEntity
                .created(location)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Subscription> updateSubscription(
            @PathVariable Long id,
            @RequestBody Subscription updated
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();

        return ResponseEntity.ok(service.update(id,updated,googleId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(
            @PathVariable Long id
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();
        service.delete(id,googleId);
        return ResponseEntity.noContent().build();
    }
}
