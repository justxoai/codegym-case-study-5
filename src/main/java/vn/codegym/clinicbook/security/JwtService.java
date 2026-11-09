package vn.codegym.clinicbook.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private static final String ALGORITHM = "HS256";

    private final SecretKey key;
    private final long expirationMs;
    private final String issuer;
    private final String audience;

    public JwtService(@Value("${app.jwt.secret}") String base64Secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs,
                      @Value("${app.jwt.issuer:clinicbook}") String issuer,
                      @Value("${app.jwt.audience:clinicbook-api}") String audience) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generateToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .audience().add(audience).and()
                .issuedAt(now)
                .notBefore(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Long extractUserId(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token);
        if (!ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
            throw new JwtException("Unexpected signing algorithm");
        }
        try {
            return Long.valueOf(jws.getPayload().getSubject());
        } catch (NumberFormatException e) {
            throw new JwtException("Invalid subject");
        }
    }
}
