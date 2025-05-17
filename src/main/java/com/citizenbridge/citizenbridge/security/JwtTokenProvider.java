//package com.citizenbridge.citizenbridge.security;
//
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import com.citizenbridge.citizenbridge.exceptions.JwtException;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.stereotype.Component;
//
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.MalformedJwtException;
//import io.jsonwebtoken.SignatureAlgorithm;
//import io.jsonwebtoken.SignatureException;
//import io.jsonwebtoken.UnsupportedJwtException;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * Provider class for JWT token generation and validation
// */
//@Component
//@Slf4j
//public class JwtTokenProvider {
//
//    @Value("${app.jwt.secret}")
//    private String jwtSecret;
//
//    @Value("${app.jwt.expiration}")
//    private long jwtExpirationInMs;
//
//    @Value("${app.jwt.refresh-expiration}")
//    private long refreshExpirationInMs;
//
//    public String generateToken(Authentication authentication) {
//        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
//
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);
//
//        String authorities = userPrincipal.getAuthorities().stream()
//                .map(GrantedAuthority::getAuthority)
//                .collect(Collectors.joining(","));
//
//        Map<String, Object> claims = new HashMap<>();
//        claims.put("roles", authorities);
//
//        return Jwts.builder()
//                .setClaims(claims)
//                .setSubject(userPrincipal.getUsername())
//                .setIssuedAt(now)
//                .setExpiration(expiryDate)
//                .signWith(SignatureAlgorithm.HS512, jwtSecret)
//                .compact();
//    }
//
//    public String generateRefreshToken(Authentication authentication) {
//        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
//
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);
//
//        return Jwts.builder()
//                .setSubject(userPrincipal.getUsername())
//                .setIssuedAt(now)
//                .setExpiration(expiryDate)
//                .signWith(SignatureAlgorithm.HS512, jwtSecret)
//                .compact();
//    }
//
//    public String getUsernameFromJWT(String token) {
//        Claims claims = Jwts.parser()
//                .setSigningKey(jwtSecret)
//                .parseClaimsJws(token)
//                .getBody();
//
//        return claims.getSubject();
//    }
//
//    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
//        final Claims claims = getAllClaimsFromToken(token);
//        return claimsResolver.apply(claims);
//    }
//
//    private Claims getAllClaimsFromToken(String token) {
//        return Jwts.parser()
//                .setSigningKey(jwtSecret)
//                .parseClaimsJws(token)
//                .getBody();
//    }
//
//    public Date getExpirationDateFromToken(String token) {
//        return getClaimFromToken(token, Claims::getExpiration);
//    }
//
//    public boolean isTokenExpired(String token) {
//        final Date expiration = getExpirationDateFromToken(token);
//        return expiration.before(new Date());
//    }
//
//    public boolean validateToken(String authToken) {
//        try {
//            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
//            return true;
//        } catch (SignatureException ex) {
//            log.error("Invalid JWT signature");
//            throw new JwtException("Invalid JWT signature");
//        } catch (MalformedJwtException ex) {
//            log.error("Invalid JWT token");
//            throw new JwtException("Invalid JWT token");
//        } catch (ExpiredJwtException ex) {
//            log.error("Expired JWT token");
//            throw new JwtException("Expired JWT token");
//        } catch (UnsupportedJwtException ex) {
//            log.error("Unsupported JWT token");
//            throw new JwtException("Unsupported JWT token");
//        } catch (IllegalArgumentException ex) {
//            log.error("JWT claims string is empty");
//            throw new JwtException("JWT claims string is empty");
//        }
//    }
//}
//
