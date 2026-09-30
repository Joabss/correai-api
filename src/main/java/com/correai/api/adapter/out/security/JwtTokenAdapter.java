package com.correai.api.adapter.out.security;

import com.correai.api.domain.model.auth.AuthToken;
import com.correai.api.domain.model.auth.InvalidTokenException;
import com.correai.api.domain.port.out.auth.TokenPort;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenAdapter implements TokenPort {

    private static final JWSAlgorithm ALGORITHM = JWSAlgorithm.HS256;

    private final byte[] secret;
    private final JwtProperties properties;
    private final Clock clock;

    @Autowired
    public JwtTokenAdapter(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtTokenAdapter(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.secret = properties.secret().getBytes();
        this.clock = clock;
    }

    @Override
    public AuthToken issue(UUID userId) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiresAt))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(ALGORITHM), claims);
            jwt.sign(new MACSigner(secret));
            return new AuthToken(jwt.serialize(), userId, expiresAt);
        } catch (JOSEException exception) {
            throw new IllegalStateException("Could not sign token", exception);
        }
    }

    @Override
    public UUID verify(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!ALGORITHM.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
                throw new InvalidTokenException("Invalid token signature");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            if (expiration == null || !expiration.toInstant().isAfter(clock.instant())) {
                throw new InvalidTokenException("Token expired");
            }
            return UUID.fromString(claims.getSubject());
        } catch (ParseException | JOSEException | IllegalArgumentException | NullPointerException exception) {
            throw new InvalidTokenException("Malformed token");
        }
    }
}
