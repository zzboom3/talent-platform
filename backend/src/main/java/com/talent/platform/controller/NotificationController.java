package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Notification;
import com.talent.platform.entity.User;
import com.talent.platform.repository.UserRepository;
import com.talent.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepo;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Result.fail(401, "请先登录"));
        }
        User user = userRepo.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Result.fail(401, "请先登录"));
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(Result.ok(notificationService.findByUserId(user.getId(), unreadOnly, pageable)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> unreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Result.fail(401, "请先登录"));
        }
        User user = userRepo.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Result.fail(401, "请先登录"));
        return ResponseEntity.ok(Result.ok(notificationService.countUnread(user.getId())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Result.fail(401, "请先登录"));
        }
        User user = userRepo.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Result.fail(401, "请先登录"));
        notificationService.markRead(id, user.getId());
        return ResponseEntity.ok(Result.ok());
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Result.fail(401, "请先登录"));
        }
        User user = userRepo.findByUsername(userDetails.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).body(Result.fail(401, "请先登录"));
        notificationService.markAllRead(user.getId());
        return ResponseEntity.ok(Result.ok());
    }
}
