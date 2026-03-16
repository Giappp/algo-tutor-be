package org.rap.algotutorbe.iam.internal.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.rap.algotutorbe.iam.internal.domain.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {
    @Value(value = "${jwt.secret}")
    private String jwtSecret;
    @Value(value = "${jwt.accessTokenExpirationMs}")
    private long accessTokenExpirationMs;

    private SecretKey secretKey;

    private Key key() {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        }
        return secretKey;
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusMillis(accessTokenExpirationMs)))
                .signWith(key())
                .compact();
    }

    public boolean isValidToken(String token) {
        Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token);
        return true;
    }

    public String getUserEmailFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}