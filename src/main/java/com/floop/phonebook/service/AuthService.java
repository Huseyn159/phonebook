package com.floop.phonebook.service;

import com.floop.phonebook.dto.AuthResponse;
import com.floop.phonebook.dto.LoginRequest;
import com.floop.phonebook.dto.RegisterRequest;

public interface AuthService {

    String register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);

}
