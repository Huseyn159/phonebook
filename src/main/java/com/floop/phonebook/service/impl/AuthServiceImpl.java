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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BadRequestException("Username is already in use");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(request.getUsername());
        userEntity.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(userEntity);

        return request.getUsername() + " is registered";
    }

    @Override
    public AuthResponse login(LoginRequest request) {


        UserEntity userEntity = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException(
                        "User with username " + request.getUsername() + " not found"
                ));


        if (!passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            throw new BadRequestException("Wrong password");
        }

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(jwtUtil.generateToken(userEntity.getUsername()));

        return authResponse;



    }
}
