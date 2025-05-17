package com.citizenbridge.citizenbridge.config;

import com.citizenbridge.citizenbridge.security.CustomUserDetailsService;
import com.citizenbridge.citizenbridge.security.JwtAuthenticationEntryPoint;
import com.citizenbridge.citizenbridge.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-Total-Count"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        auth
                                // Public endpoints - no authentication required
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/v1/auth/**").permitAll()
                                .requestMatchers("/api/public/**").permitAll()

                                // Swagger/OpenAPI endpoints
                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                                .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()

                                // Health check endpoints
                                .requestMatchers("/actuator/health").permitAll()

                                // User Management endpoints - Administrative access
                                .requestMatchers(HttpMethod.POST, "/api/users/admin/create").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole("ADMIN", "AGENCY_ADMIN", "MODERATOR")
                                .requestMatchers(HttpMethod.GET, "/api/users/{userId}").hasAnyRole("ADMIN", "AGENCY_ADMIN", "MODERATOR", "SUPPORT")
                                .requestMatchers(HttpMethod.PUT, "/api/users/{userId}").hasAnyRole("ADMIN", "AGENCY_ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/users/{userId}/status").hasAnyRole("ADMIN", "AGENCY_ADMIN", "MODERATOR")
                                .requestMatchers(HttpMethod.PUT, "/api/users/{userId}/roles").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "/api/users/{userId}/reset-password").hasAnyRole("ADMIN", "AGENCY_ADMIN", "SUPPORT")
                                .requestMatchers(HttpMethod.POST, "/api/users/{userId}/image").hasAnyRole("ADMIN", "AGENCY_ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/users/{userId}/image-upload-url").hasAnyRole("ADMIN", "AGENCY_ADMIN")

                                // User profile endpoints - authenticated users
                                .requestMatchers("/api/users/profile/**").authenticated()

                                // All other endpoints require authentication
                                .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}