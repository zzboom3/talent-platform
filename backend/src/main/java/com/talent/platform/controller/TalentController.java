package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.TalentProfile;
import com.talent.platform.entity.User;
import com.talent.platform.repository.TalentProfileRepository;
import com.talent.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/talents")
@RequiredArgsConstructor
public class TalentController {

    private final TalentProfileRepository talentRepo;
    private final UserRepository userRepo;

    @GetMapping
    public Result<List<TalentProfile>> list() {
        return Result.ok(talentRepo.findAll());
    }

    @GetMapping("/{id}")
    public Result<TalentProfile> getById(@PathVariable Long id) {
        return talentRepo.findById(id)
                .map(Result::ok)
                .orElse(Result.fail("人才档案不存在"));
    }

    @GetMapping("/my")
    public Result<TalentProfile> getMy(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        return talentRepo.findByUser(user)
                .map(Result::ok)
                .orElse(Result.fail("尚未创建档案"));
    }

    @PostMapping
    public Result<TalentProfile> create(@RequestBody TalentProfile profile,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        if (talentRepo.findByUser(user).isPresent()) {
            return Result.fail("档案已存在，请使用编辑接口");
        }
        profile.setUser(user);
        return Result.ok(talentRepo.save(profile));
    }

    @PutMapping("/{id}")
    public Result<TalentProfile> update(@PathVariable Long id,
                                        @RequestBody TalentProfile updated,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        TalentProfile profile = talentRepo.findById(id).orElse(null);
        if (profile == null) return Result.fail("档案不存在");
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        boolean isOwner = profile.getUser().getId().equals(user.getId());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) return Result.fail("无权限");

        profile.setRealName(updated.getRealName());
        profile.setSkills(updated.getSkills());
        profile.setEducation(updated.getEducation());
        profile.setExperience(updated.getExperience());
        profile.setCity(updated.getCity());
        profile.setAvatarUrl(updated.getAvatarUrl());
        return Result.ok(talentRepo.save(profile));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id,
                            @AuthenticationPrincipal UserDetails userDetails) {
        TalentProfile profile = talentRepo.findById(id).orElse(null);
        if (profile == null) return Result.fail("档案不存在");
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        boolean isOwner = profile.getUser().getId().equals(user.getId());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) return Result.fail("无权限");
        talentRepo.deleteById(id);
        return Result.ok();
    }
}
