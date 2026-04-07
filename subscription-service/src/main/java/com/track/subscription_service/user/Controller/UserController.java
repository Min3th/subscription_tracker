package com.track.subscription_service.user.Controller;

import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepo;

    public UserController(UserRepository user) {
        this.userRepo = user;
    }

    @GetMapping
    public List<User> getAll(){
        return userRepo.findAll();
    }

    @PostMapping
    public User createUser(@RequestBody User user){
        return userRepo.save(user);
    }

}
