package com.floop.phonebook.service.impl;

import com.floop.phonebook.config.JwtUtil;
import com.floop.phonebook.dto.AuthResponse;
import com.floop.phonebook.dto.LoginRequest;
import com.floop.phonebook.dto.RegisterRequest;
import com.floop.phonebook.entity.UserEntity;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.repository.UserRepository;
import com.floop.phonebook.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public String register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("Username is already in use");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(request.getUsername());
        userEntity.setPassword(passwordEncoder.encode(request.getPassword()));
        userEntity.setEmail(request.getEmail());

        userRepository.save(userEntity);

        return request.getUsername() + " is registered";
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(jwtUtil.generateToken(authentication.getName()));
        return authResponse;

    }
}
