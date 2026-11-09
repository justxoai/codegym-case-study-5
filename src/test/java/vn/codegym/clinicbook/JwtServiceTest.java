package vn.codegym.clinicbook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import vn.codegym.clinicbook.security.JwtService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SECRET = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
    private static final long ONE_DAY = 86_400_000L;

    private final JwtService service = new JwtService(SECRET, ONE_DAY, "clinicbook", "clinicbook-api");

    private static JsonNode part(String token, int index) throws Exception {
        String raw = token.split("\\.")[index];
        return JSON.readTree(new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8));
    }

    @Test
    void tokenIsSignedWithHs256() throws Exception {
        String token = service.generateToken(42L);
        assertEquals("HS256", part(token, 0).get("alg").asText());
    }

    @Test
    void payloadHasOnlyStandardClaimsAndNoPersonalData() throws Exception {
        String token = service.generateToken(42L);
        JsonNode payload = part(token, 1);

        Set<String> names = JSON.convertValue(payload, java.util.Map.class).keySet();
        assertEquals(Set.of("iss", "sub", "aud", "iat", "nbf", "exp", "jti"), names);

        assertEquals("clinicbook", payload.get("iss").asText());
        assertEquals("42", payload.get("sub").asText());
        assertTrue(payload.get("aud").toString().contains("clinicbook-api"));
        assertEquals(ONE_DAY / 1000, payload.get("exp").asLong() - payload.get("iat").asLong());
    }

    @Test
    void everyTokenHasItsOwnId() throws Exception {
        String first = part(service.generateToken(1L), 1).get("jti").asText();
        String second = part(service.generateToken(1L), 1).get("jti").asText();
        assertNotEquals(first, second);
    }

    @Test
    void userIdComesBackOutOfTheToken() {
        assertEquals(42L, service.extractUserId(service.generateToken(42L)));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = service.generateToken(42L);
        String[] parts = token.split("\\.");
        String forged = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"iss\":\"clinicbook\",\"sub\":\"1\",\"aud\":\"clinicbook-api\"}".getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];
        assertThrows(JwtException.class, () -> service.extractUserId(forged));
        assertThrows(JwtException.class, () -> service.extractUserId("not.a.jwt"));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        SecretKey other = Jwts.SIG.HS256.key().build();
        String token = Jwts.builder().issuer("clinicbook").subject("1").audience().add("clinicbook-api").and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(other, Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> service.extractUserId(token));
    }

    @Test
    void tokenSignedWithAnotherHmacAlgorithmIsRejectedEvenWithTheRightKey() {
        byte[] longKey = new byte[64];
        java.util.Arrays.fill(longKey, (byte) 7);
        JwtService longKeyService = new JwtService(Encoders.BASE64.encode(longKey), ONE_DAY, "clinicbook", "clinicbook-api");
        String hs512 = Jwts.builder().issuer("clinicbook").subject("1").audience().add("clinicbook-api").and()
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(longKey), Jwts.SIG.HS512).compact();
        assertThrows(JwtException.class, () -> longKeyService.extractUserId(hs512));
        assertEquals(1L, longKeyService.extractUserId(longKeyService.generateToken(1L)));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService(SECRET, -1000L, "clinicbook", "clinicbook-api");
        assertThrows(JwtException.class, () -> service.extractUserId(shortLived.generateToken(1L)));
    }

    @Test
    void tokenForAnotherIssuerOrAudienceIsRejected() {
        JwtService otherIssuer = new JwtService(SECRET, ONE_DAY, "someone-else", "clinicbook-api");
        JwtService otherAudience = new JwtService(SECRET, ONE_DAY, "clinicbook", "another-api");
        assertThrows(JwtException.class, () -> service.extractUserId(otherIssuer.generateToken(1L)));
        assertThrows(JwtException.class, () -> service.extractUserId(otherAudience.generateToken(1L)));
    }
}
