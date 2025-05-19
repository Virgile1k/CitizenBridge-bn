package com.citizenbridge.citizenbridge.security;

import com.citizenbridge.citizenbridge.model.Users;
import com.citizenbridge.citizenbridge.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users users = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Create a single authority from the user's role
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + users.getRole().name());

        return org.springframework.security.core.userdetails.User
                .withUsername(users.getUsername())
                .password(users.getPasswordHash())
                .authorities(Collections.singletonList(authority))
                .build();
    }
}
