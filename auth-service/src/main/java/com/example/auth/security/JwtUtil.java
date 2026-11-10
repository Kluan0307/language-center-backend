package com.example.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private long expiration;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  public String generateToken(
      String username, String role, Long accountId, Long profileId, Long branchId) {
    return generateToken(username, role, accountId, profileId, branchId, 0);
  }

  public String generateToken(
      String username,
      String role,
      Long accountId,
      Long profileId,
      Long branchId,
      long sessionVersion) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration);

    return Jwts.builder()
        .setSubject(username)
        .claim("role", role)
        .claim("accountId", accountId)
        .claim("profileId", profileId)
        .claim("branchId", branchId)
        .claim("sv", sessionVersion)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public long extractSessionVersion(String token) {
    return extractClaim(
        token,
        claims -> {
          Object value = claims.get("sv");
          if (value == null) return 0L; // Existing pre-migration tokens are version zero.
          if (!(value instanceof Number) || !value.toString().matches("[0-9]+"))
            throw new IllegalArgumentException("Invalid session version");
          return Long.valueOf(value.toString());
        });
  }

  public Long extractBranchId(String token) {
    return extractClaim(token, claims -> claims.get("branchId", Long.class));
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
  }

  public Long extractAccountId(String token) {
    return extractClaim(token, claims -> claims.get("accountId", Long.class));
  }

  public Long extractProfileId(String token) {
    return extractClaim(token, claims -> claims.get("profileId", Long.class));
  }

  public boolean isTokenExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(new Date());
  }

  public boolean validateToken(String token, String username) {
    return extractUsername(token).equals(username) && !isTokenExpired(token);
  }

  private <T> T extractClaim(String token, Function<Claims, T> resolver) {
    Claims claims =
        Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    return resolver.apply(claims);
  }
}
