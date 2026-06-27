package com.kailash.storageservice.service;

import com.kailash.storageservice.dto.LoginRequest;
import com.kailash.storageservice.dto.LoginResponse;
import com.kailash.storageservice.dto.RegisterRequest;
import com.kailash.storageservice.exception.InvalidCredentialsException;
import com.kailash.storageservice.exception.UserAlreadyExists;
import com.kailash.storageservice.jwt.JwtService;
import com.kailash.storageservice.model.Role;
import com.kailash.storageservice.model.User;
import com.kailash.storageservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtService=jwtService;
    }

    public void register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new UserAlreadyExists("Email already exists");
        }

        User user=new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request){
        User user=userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new InvalidCredentialsException("Invalid email or password"));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token= jwtService.generateToken(user.getEmail());

        return new LoginResponse(
                token,
                "Bearer"
        );
    }
}
