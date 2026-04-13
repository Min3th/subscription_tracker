package com.track.subscription_service.subscription.service;

import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository repo;
    private final UserRepository userRepository;

    public SubscriptionService(SubscriptionRepository repo, UserRepository userRepository) {
        this.repo = repo;
        this.userRepository = userRepository;
    }

    public List<Subscription> getAll(){
        return repo.findAll();
    }

    public Subscription create(Subscription subscription,String googleId){
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        subscription.setUser(user);
        return repo.save(subscription);
    }

    public List<Subscription> getByGoogleId(String googleId){
        return repo.findByUser_GoogleId(googleId);
    }

    public Subscription getByIdAndGoogleId(Long id, String googleId){
        return repo.findByIdAndUser_GoogleId(id, googleId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
    }

    public Subscription update(Long id,Subscription updated,String googleId){
        Subscription existing = getByIdAndGoogleId(id,googleId);

        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setCost(updated.getCost());
        existing.setDuration(updated.getDuration());
        existing.setType(updated.getType());

        return repo.save(existing);
    }
}
