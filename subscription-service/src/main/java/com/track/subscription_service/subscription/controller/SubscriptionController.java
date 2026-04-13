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
    private final JwtService jwtService;

    public SubscriptionController(SubscriptionService service, JwtService jwtService){
        this.service = service;
        this.jwtService = jwtService;
    }

    @GetMapping
    public List<Subscription> getUserSubscriptions(@RequestHeader("Authorization") String authHeader){
        String token = authHeader.replace("Bearer ","");
        String googleId = jwtService.extractGoogleId(token);

        return service.getByGoogleId(googleId);

    }

    @PostMapping
    public Subscription createSubscription(
            @RequestBody Subscription subscription,
            @RequestHeader("Authorization") String authHeader){

        String token = authHeader.replace("Bearer ",""); // to remove Bearer part in token
        String googleId = jwtService.extractGoogleId(token);
        return service.create(subscription,googleId);
    }

    @PutMapping("/{id}")
    public Subscription updateSubscription(
            @PathVariable Long id,
            @RequestBody Subscription updated,
            @RequestHeader("Authorization") String authHeader
    ){
        String token = authHeader.replace("Bearer ","");
        String googleId = jwtService.extractGoogleId(token);

        return service.update(id,updated,googleId);
    }

    @DeleteMapping("/{id}")
    public void deleteSubscription(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ){
        String token = authHeader.replace("Bearer ","");
        String googleId = jwtService.extractGoogleId(token);
        service.delete(id,googleId);
    }
}
