package com.track.subscription_service.subscription.controller;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.subscription.service.SubscriptionService;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service){
        this.service = service;
    }

    @GetMapping
    public List<Subscription> getAll(){
        return service.getAll();
    }

    @PostMapping
    public Subscription createSubscription(@RequestBody Subscription subscription){
        return service.create(subscription);
    }
}
