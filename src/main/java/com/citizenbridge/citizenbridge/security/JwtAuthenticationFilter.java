package com.citizenbridge.citizenbridge.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Extract token from Authorization header
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);

            try {
                // Only attempt to extract username if token is not empty
                if (!jwt.trim().isEmpty()) {
                    username = jwtUtil.extractUsername(jwt);
                }
            } catch (IllegalArgumentException e) {
                logger.error("Unable to get JWT Token: {}", e.getMessage());
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token has expired: {}", e.getMessage());
            } catch (MalformedJwtException e) {
                logger.error("JWT Token is malformed: {}", e.getMessage());
                // Log additional details for debugging (be careful not to log sensitive data in production)
                logger.debug("Malformed token details - Length: {}, First 20 chars: {}",
                        jwt.length(),
                        jwt.length() > 20 ? jwt.substring(0, 20) + "..." : jwt);
            } catch (UnsupportedJwtException e) {
                logger.error("JWT Token is unsupported: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("JWT Token validation error: {}", e.getMessage());
            }
        } else if (authorizationHeader != null) {
            logger.warn("Authorization header does not start with 'Bearer ': {}", authorizationHeader);
        }

        // Validate token and set authentication context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    logger.debug("Authentication set for user: {}", username);
                } else {
                    logger.warn("JWT Token validation failed for user: {}", username);
                }
            } catch (Exception e) {
                logger.error("Error loading user details for username: {}", username, e);
            }
        }

        // Continue with the filter chain
        chain.doFilter(request, response);
    }
}