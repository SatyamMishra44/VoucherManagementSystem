package com.example.Voucher.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);
    private static final int HS256_MIN_KEY_BYTES = 32;
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(normalizeSecret(properties.getSecret()));
    }

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, properties.getAccessExpirationSeconds(), ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, properties.getRefreshExpirationSeconds(), REFRESH_TOKEN_TYPE);
    }

    private String generateToken(UserDetails userDetails, long expirationSeconds, String tokenType) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);

        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.getIssuer())
                .subject(userDetails.getUsername())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256);
        if (userDetails instanceof TenantAwareUserDetails tenantAwareUserDetails) {
            builder.claim(TENANT_ID_CLAIM, tenantAwareUserDetails.getTenantId());
        }
        return builder.compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        if (!ACCESS_TOKEN_TYPE.equals(extractTokenType(token))) {
            return false;
        }
        String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }
        return hasMatchingTenant(token, userDetails);
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        if (!REFRESH_TOKEN_TYPE.equals(extractTokenType(token))) {
            return false;
        }
        String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }
        return hasMatchingTenant(token, userDetails);
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token).get(TOKEN_TYPE_CLAIM, String.class);
    }

    public Long extractTenantId(String token) {
        Object tenantId = extractAllClaims(token).get(TENANT_ID_CLAIM);
        if (tenantId instanceof Integer value) {
            return value.longValue();
        }
        if (tenantId instanceof Long value) {
            return value;
        }
        return null;
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean hasMatchingTenant(String token, UserDetails userDetails) {
        if (!(userDetails instanceof TenantAwareUserDetails tenantAwareUserDetails)) {
            return true;
        }
        Long tokenTenantId = extractTenantId(token);
        return tokenTenantId != null && tokenTenantId.equals(tenantAwareUserDetails.getTenantId());
    }

    private byte[] normalizeSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret must not be blank");
        }

        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= HS256_MIN_KEY_BYTES) {
            return raw;
        }

        LOGGER.warn("JWT secret is shorter than 32 bytes. Normalizing it to a 256-bit key. Use a 32+ byte JWT secret in production.");
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to normalize JWT secret", ex);
        }
    }
}
