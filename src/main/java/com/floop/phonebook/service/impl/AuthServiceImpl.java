package com.floop.phonebook.service.impl;

import com.floop.phonebook.config.JwtUtil;
import com.floop.phonebook.dto.AuthResponse;
import com.floop.phonebook.dto.LoginRequest;
import com.floop.phonebook.dto.RegisterRequest;
import com.floop.phonebook.entity.UserEntity;
import com.floop.phonebook.exception.BadRequestException;
import com.floop.phonebook.exception.UnauthorizedException;
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
    private final String dummyHash;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.dummyHash = passwordEncoder.encode("timing-attack-dummy");
    }

    private static final String INVALID_CREDENTIALS = "Invalid username or password";

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

        UserEntity user = userRepository.findByUsername(request.getUsername()).orElse(null);

        String hash = (user!=null) ? user.getPassword() : dummyHash;
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(),hash);

        if (user == null || !passwordMatches){
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(jwtUtil.generateToken(user.getUsername()));

        return authResponse;



    }
}
