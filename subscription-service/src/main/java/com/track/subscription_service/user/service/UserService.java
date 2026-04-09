package com.track.subscription_service.user.service;

import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public List<User> getAll(){
        return repo.findAll();
    }

    public User create(User user){
        return repo.save(user);
    }
}
