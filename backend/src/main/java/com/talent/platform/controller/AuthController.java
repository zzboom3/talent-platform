package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.dto.LoginRequest;
import com.talent.platform.dto.RegisterRequest;
import com.talent.platform.entity.User;
import com.talent.platform.repository.UserRepository;
import com.talent.platform.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return Result.fail("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        try {
            user.setRole(User.Role.valueOf(req.getRole().toUpperCase()));
        } catch (Exception e) {
            user.setRole(User.Role.TALENT);
        }
        userRepository.save(user);
        return Result.ok("注册成功");
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElse(null);
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return Result.fail("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return Result.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "userId", user.getId()
        ));
    }
}
