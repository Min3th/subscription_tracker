package com.track.subscription_service.subscription.service;

import com.track.subscription_service.subscription.dto.CreateSubscriptionRequest;
import com.track.subscription_service.subscription.dto.SubscriptionResponse;
import com.track.subscription_service.subscription.dto.UpdateSubscriptionRequest;
import com.track.subscription_service.subscription.entity.Subscription;
import com.track.subscription_service.subscription.repository.SubscriptionRepository;
import com.track.subscription_service.subscription.model.SubscriptionType;
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

    public Subscription create(CreateSubscriptionRequest request, String googleId){
        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Subscription subscription = new Subscription();
        subscription.setName(request.name());
        subscription.setCost(request.cost());
        subscription.setType(request.type());
        subscription.setDuration(request.duration());
        subscription.setCategory(request.category());
        subscription.setDescription(request.description());
        subscription.setPaymentMethod(request.paymentMethod());
        subscription.setWebsite(request.website());
        subscription.setStartDate(request.startDate());
        subscription.setBillingIntervalUnit(request.type() == SubscriptionType.RECURRING ? request.billingIntervalUnit() : null);
        subscription.setBillingIntervalCount(request.type() == SubscriptionType.RECURRING ? request.billingIntervalCount() : null);
        subscription.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
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

    public Subscription update(Long id, UpdateSubscriptionRequest request, String googleId){
        Subscription existing = getByIdAndGoogleId(id,googleId);

        existing.setName(request.name());
        existing.setCost(request.cost());
        existing.setType(request.type());
        existing.setDuration(request.duration());
        existing.setCategory(request.category());
        existing.setDescription(request.description());
        existing.setPaymentMethod(request.paymentMethod());
        existing.setWebsite(request.website());
        existing.setStartDate(request.startDate());
        existing.setBillingIntervalUnit(request.type() == SubscriptionType.RECURRING ? request.billingIntervalUnit() : null);
        existing.setBillingIntervalCount(request.type() == SubscriptionType.RECURRING ? request.billingIntervalCount() : null);
        existing.setEmailNotificationsEnabled(request.emailNotificationsEnabled());

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

        if (subscription.getType() == SubscriptionType.RECURRING) {
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
        } else {
            res.nextBillingDate = null;
            res.totalPaid = subscription.getStartDate().isAfter(LocalDate.now()) ? 0 : subscription.getCost();
        }

        return res;
    }
}
