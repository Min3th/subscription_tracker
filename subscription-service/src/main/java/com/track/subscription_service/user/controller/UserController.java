package com.track.subscription_service.user.controller;

import com.track.subscription_service.user.entity.User;
import com.track.subscription_service.user.repository.UserRepository;
import com.track.subscription_service.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

//    @GetMapping
//    public List<User> getAll(){
//        return service.getAll();
//    }


}
