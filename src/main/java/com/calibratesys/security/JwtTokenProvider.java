package com.calibratesys.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Utility component for generating, signing, parsing, and validating JSON Web Tokens (JWT).
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey signingKey;
    private final long expirationMs;

    public static final String DEFAULT_DEV_SECRET = "c21hcnQtbGFib3JhdG9yeS1lcXVpcG1lbnQtY2FsaWJyYXRpb24tc3lzdGVtLXNlY3JldC1rZXktMjAyNg==";

    public JwtTokenProvider(
            @Value("${app.jwt.secret:c21hcnQtbGFib3JhdG9yeS1lcXVpcG1lbnQtY2FsaWJyYXRpb24tc3lzdGVtLXNlY3JldC1rZXktMjAyNg==}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs,
            org.springframework.core.env.Environment env) {
        validateSecretConfiguration(secret, env);
        this.signingKey = deriveKey(secret);
        this.expirationMs = expirationMs;
    }

    private void validateSecretConfiguration(String secret, org.springframework.core.env.Environment env) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT configuration error: app.jwt.secret cannot be empty or null.");
        }

        boolean isProd = false;
        if (env != null && env.getActiveProfiles() != null) {
            for (String profile : env.getActiveProfiles()) {
                if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                    isProd = true;
                    break;
                }
            }
        }

        if (isProd) {
            if (DEFAULT_DEV_SECRET.equals(secret.trim()) || secret.trim().length() < 32) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: Unsafe JWT secret configuration in production profile! A strong, unique JWT_SECRET of at least 32 characters must be provided via environment variables.");
            }
        }
    }

    private SecretKey deriveKey(String secret) {
        try {
            // Ensure minimum 256-bit key length via SHA-256 derivation
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT signing key", e);
        }
    }

    /**
     * Generate a signed JWT token from an authenticated user.
     */
    public String generateToken(Authentication authentication) {
        CustomUserDetails userPrincipal = (CustomUserDetails) authentication.getPrincipal();
        return generateTokenFromUserDetails(userPrincipal);
    }

    /**
     * Generate a signed JWT token directly from user details.
     */
    public String generateTokenFromUserDetails(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("staffId", userDetails.getStaffId())
                .claim("userId", userDetails.getId())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract username/email subject from JWT token.
     */
    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Validate signature and expiration of JWT token.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token format: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty or null: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation error: {}", ex.getMessage());
        }
        return false;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
