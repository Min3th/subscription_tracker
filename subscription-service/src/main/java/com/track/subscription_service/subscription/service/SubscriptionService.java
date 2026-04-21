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

//        if ("recurring".equalsIgnoreCase(updated.getType())) {
//            if (updated.getStartDate() == null ||
//                    updated.getBillingIntervalUnit() == null ||
//                    updated.getBillingIntervalCount() == null) {
//
//                throw new RuntimeException("Recurring subscriptions require billing details");
//            }
//        }

        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getCategory() != null) existing.setCategory(updated.getCategory());
        if (updated.getCost() != null) existing.setCost(updated.getCost());
        if (updated.getDuration() != null) existing.setDuration(updated.getDuration());
        if (updated.getType() != null) existing.setType(updated.getType());

        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getPaymentMethod() != null) existing.setPaymentMethod(updated.getPaymentMethod());
        if (updated.getWebsite() != null) existing.setWebsite(updated.getWebsite());
        if (updated.getStartDate() != null) existing.setStartDate(updated.getStartDate());
        if (updated.getBillingIntervalUnit() != null) existing.setBillingIntervalUnit(updated.getBillingIntervalUnit());
        if (updated.getBillingIntervalCount() != null) existing.setBillingIntervalCount(updated.getBillingIntervalCount());

        return repo.save(existing);
    }

    public void delete(Long id,String googleId){
        Subscription existing = getByIdAndGoogleId(id,googleId);
        repo.delete(existing);
    }
}
