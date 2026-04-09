package com.track.subscription_service.auth.service;

import com.track.subscription_service.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    private final String SECRET_KEY = "your-super-secret-key-change-this";

    public String generateToken(User user) {

        return Jwts.builder()
                .setSubject(user.getGoogleId()) // unique identifier
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24h
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }
}