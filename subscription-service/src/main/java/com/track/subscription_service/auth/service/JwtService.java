package com.track.subscription_service.auth.service;

import com.track.subscription_service.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(User user) {

        return Jwts.builder()
                .setSubject(user.getGoogleId()) // unique identifier
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // 15 min
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user){
        return Jwts.builder()
                .setSubject(user.getGoogleId())
                .claim("type","refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+ 1000L*60*60*24*7)) // 7 days
                .signWith(key)
                .compact();
    }

    public String extractGoogleId(String token){
        return extractAllClaims(token).getSubject();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @PostConstruct
    public void debugSecret() {
        System.out.println("JWT SECRET LENGTH: " + key.getEncoded().length);
        System.out.println("JWT SECRET RAW: " + new String(key.getEncoded()));
    }
}