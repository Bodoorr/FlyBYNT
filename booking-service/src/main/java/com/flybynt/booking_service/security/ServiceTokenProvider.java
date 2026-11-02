package com.flybynt.booking_service.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Short-lived token for calls made without a user, e.g. the PENDING-booking reconciler. */
@Component
public class ServiceTokenProvider {
    private final JwtEncoder encoder;
    public ServiceTokenProvider(JwtEncoder encoder){
        this.encoder=encoder;
    }

    public String token(){
        Instant now=Instant.now();
        JwtClaimsSet claims=JwtClaimsSet.builder()
                .issuer("booking-service").subject("booking-service").claim("role","SERVICE")
                .issuedAt(now).expiresAt(now.plus(Duration.ofMinutes(5))).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
}
