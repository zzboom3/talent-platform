package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.dto.LoginRequest;
import com.talent.platform.dto.RegisterRequest;
import com.talent.platform.entity.Company;
import com.talent.platform.entity.TalentProfile;
import com.talent.platform.entity.User;
import com.talent.platform.repository.CompanyRepository;
import com.talent.platform.repository.TalentProfileRepository;
import com.talent.platform.repository.UserRepository;
import com.talent.platform.service.NotificationService;
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
    private final TalentProfileRepository talentProfileRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;

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
        notificationService.notifyNewUserRegister(user);
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
        notificationService.notifyWelcome(user);
        String avatarUrl = null;
        if (user.getRole() == User.Role.TALENT) {
            avatarUrl = talentProfileRepository.findByUser(user)
                    .map(TalentProfile::getAvatarUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .orElse(null);
        } else if (user.getRole() == User.Role.ENTERPRISE) {
            avatarUrl = companyRepository.findByUser(user)
                    .map(Company::getLogoUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .orElse(null);
        }
        return Result.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole().name(),
                "userId", user.getId(),
                "avatarUrl", avatarUrl != null ? avatarUrl : ""
        ));
    }
}
