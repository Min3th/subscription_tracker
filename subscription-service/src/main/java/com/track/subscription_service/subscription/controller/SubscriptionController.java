package com.track.subscription_service.subscription.controller;

import com.track.subscription_service.auth.service.JwtService;
import com.track.subscription_service.subscription.dto.SubscriptionResponse;
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
    public ResponseEntity<List<SubscriptionResponse>>getUserSubscriptions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();

        List<SubscriptionResponse> response = service.getByGoogleId(googleId)
                .stream()
                .map(service::mapToResponse)
                .toList();

        return ResponseEntity.ok(response);

    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getSubscription(
            @PathVariable Long id,
            Authentication auth
    ){
        Subscription subscription = service.getByIdAndGoogleId(id, auth.getName());
        return ResponseEntity.ok(service.mapToResponse(subscription));
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @RequestBody Subscription subscription){

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();
        Subscription created = service.create(subscription,googleId);
        URI location = URI.create("/subscriptions/" + created.getId());

        return ResponseEntity
                .created(location)
                .body(service.mapToResponse(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @RequestBody Subscription updated
    ){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String googleId = auth.getName();

        return ResponseEntity.ok(service.mapToResponse(service.update(updated.getId(), updated, googleId)));
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
