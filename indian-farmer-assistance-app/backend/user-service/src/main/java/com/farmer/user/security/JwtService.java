package com.farmer.user.security;

import com.farmer.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT token generation and validation.
 * Requirements: 11.6
 */
@Service
@Slf4j
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    @Value("${app.jwt.issuer:indian-farmer-assistance}")
    private String issuer;

    /**
     * Get signing key from secret.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate access token for a user.
     * Requirements: 11.6
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("farmerId", user.getFarmerId());
        claims.put("phone", user.getPhone());
        return createToken(claims, user.getFarmerId(), jwtExpiration);
    }

    /**
     * Generate refresh token for a user.
     * Requirements: 11.6
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return createToken(claims, user.getFarmerId(), refreshExpiration);
    }

    /**
     * Create a JWT token with claims and subject.
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expiration, ChronoUnit.MILLIS)))
                .signWith(getSigningKey());
        
        // Add custom claims
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        
        return builder.compact();
    }

    /**
     * Extract username (farmer ID) from token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract role from token.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Extract farmer ID from token.
     */
    public String extractFarmerId(String token) {
        return extractClaim(token, claims -> claims.get("farmerId", String.class));
    }

    /**
     * Extract a specific claim from token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token.
     */
    private Claims extractAllClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validate token against user.
     * Requirements: 11.6
     */
    public boolean validateToken(String token, User user) {
        try {
            final String username = extractUsername(token);
            return (username.equals(user.getFarmerId()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token structure and signature.
     * Requirements: 11.6
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Get token expiration time in seconds.
     */
    public long getExpirationSeconds() {
        return jwtExpiration / 1000;
    }

    /**
     * Get refresh token expiration time in seconds.
     */
    public long getRefreshExpirationSeconds() {
        return refreshExpiration / 1000;
    }

    /**
     * Inner class representing farmer user details extracted from JWT token.
     * Used for authentication and authorization purposes.
     */
    public static class FarmerUserDetails implements org.springframework.security.core.userdetails.UserDetails {
        private final String farmerId;
        private final String phone;
        private final String role;
        private final boolean isActive;

        public FarmerUserDetails(String farmerId, String phone, String role, boolean isActive) {
            this.farmerId = farmerId;
            this.phone = phone;
            this.role = role;
            this.isActive = isActive;
        }

        public String getFarmerId() {
            return farmerId;
        }

        @Override
        public String getUsername() {
            return farmerId;
        }

        @Override
        public String getPassword() {
            return null; // JWT-based authentication doesn't use passwords
        }

        public String getPhone() {
            return phone;
        }

        public String getRole() {
            return role;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return isActive;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return isActive;
        }

        @Override
        public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
            return java.util.Collections.singletonList(
                    new org.springframework.security.core.GrantedAuthority() {
                        @Override
                        public String getAuthority() {
                            return "ROLE_" + role;
                        }
                    }
            );
        }
    }
}