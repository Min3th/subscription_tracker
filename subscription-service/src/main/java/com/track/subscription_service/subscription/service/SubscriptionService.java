package com.track.subscription_service.subscription.service;

import com.track.subscription_service.subscription.dto.SubscriptionResponse;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository repo;
    private final UserRepository userRepository;
    private final BillingService billingService;

    public SubscriptionService(
            SubscriptionRepository repo,
            UserRepository userRepository,
            BillingService billingService
    ) {
        this.repo = repo;
        this.userRepository = userRepository;
        this.billingService = billingService;
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
        if (updated.isEmailNotificationsEnabled() != null) {
            existing.setEmailNotificationsEnabled(updated.isEmailNotificationsEnabled());
        }

        return repo.save(existing);
    }

    public void delete(Long id,String googleId){
        Subscription existing = getByIdAndGoogleId(id,googleId);
        repo.delete(existing);
    }

    public SubscriptionResponse mapToResponse(Subscription subscription){
        SubscriptionResponse res = new SubscriptionResponse();

        res.id =subscription.getId();
        res.name = subscription.getName();
        res.cost = subscription.getCost();
        res.type = subscription.getType();
        res.duration = subscription.getDuration();
        res.category = subscription.getCategory();
        res.description = subscription.getDescription();
        res.paymentMethod = subscription.getPaymentMethod();
        res.website = subscription.getWebsite();
        res.billingIntervalUnit = subscription.getBillingIntervalUnit();
        res.billingIntervalCount =subscription.getBillingIntervalCount();
        res.startDate = subscription.getStartDate();

        res.nextBillingDate = billingService.getNextBillingDate(
                subscription.getStartDate(),
                subscription.getBillingIntervalUnit(),
                subscription.getBillingIntervalCount()
        );

        res.totalPaid = billingService.calculateTotalPaid(
                subscription.getStartDate(),
                subscription.getBillingIntervalUnit(),
                subscription.getBillingIntervalCount(),
                subscription.getCost()
        );

        return res;
    }
}
