package com.track.subscription_service.user.repository;

import com.track.subscription_service.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}
