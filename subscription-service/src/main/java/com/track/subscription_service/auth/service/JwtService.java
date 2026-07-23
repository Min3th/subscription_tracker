package com.track.subscription_service.auth.service;

import com.track.subscription_service.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final String audience;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.issuer}") String issuer,
                      @Value("${jwt.audience}") String audience) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateAccessToken(User user) {

        return Jwts.builder()
                .setSubject(user.getGoogleId()) // unique identifier
                .setId(UUID.randomUUID().toString())
                .setIssuer(issuer)
                .setAudience(audience)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 15)) // 15 min
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user, String tokenId){
        return Jwts.builder()
                .setSubject(user.getGoogleId())
                .setId(tokenId)
                .setIssuer(issuer)
                .setAudience(audience)
                .claim("type","refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+ 1000L*60*60*24*7)) // 7 days
                .signWith(key)
                .compact();
    }

    public String extractGoogleId(String token){
        return extractAllClaims(token).getSubject();
    }

    public Claims validateAccessToken(String token) {
        Claims claims = extractAllClaims(token);
        if (!"access".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Token is not an access token");
        }
        return claims;
    }

    public Claims validateRefreshToken(String token) {
        Claims claims = extractAllClaims(token);
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Token is not a refresh token");
        }
        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new IllegalArgumentException("Refresh token has no identifier");
        }
        return claims;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
