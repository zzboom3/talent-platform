package com.talent.platform.controller;

import com.talent.platform.common.Result;
import com.talent.platform.entity.Course;
import com.talent.platform.entity.CourseEnrollment;
import com.talent.platform.entity.User;
import com.talent.platform.repository.CourseEnrollmentRepository;
import com.talent.platform.repository.CourseRepository;
import com.talent.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

    private final CourseRepository courseRepo;
    private final CourseEnrollmentRepository enrollRepo;
    private final UserRepository userRepo;

    @GetMapping("/courses")
    public Result<List<Course>> list() {
        return Result.ok(courseRepo.findAll());
    }

    @GetMapping("/courses/{id}")
    public Result<Course> getById(@PathVariable Long id) {
        return courseRepo.findById(id).map(Result::ok).orElse(Result.fail("课程不存在"));
    }

    @PostMapping("/courses")
    public Result<Course> create(@RequestBody Course course) {
        return Result.ok(courseRepo.save(course));
    }

    @PutMapping("/courses/{id}")
    public Result<Course> update(@PathVariable Long id, @RequestBody Course updated) {
        Course course = courseRepo.findById(id).orElse(null);
        if (course == null) return Result.fail("课程不存在");
        course.setTitle(updated.getTitle());
        course.setDescription(updated.getDescription());
        course.setTeacher(updated.getTeacher());
        course.setCategory(updated.getCategory());
        course.setCoverUrl(updated.getCoverUrl());
        course.setVideoUrl(updated.getVideoUrl());
        course.setDuration(updated.getDuration());
        course.setChapterCount(updated.getChapterCount());
        return Result.ok(courseRepo.save(course));
    }

    @DeleteMapping("/courses/{id}")
    public Result<?> delete(@PathVariable Long id) {
        courseRepo.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/enrollments")
    public Result<CourseEnrollment> enroll(@RequestBody Map<String, Long> body,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        Long courseId = body.get("courseId");
        Course course = courseRepo.findById(courseId).orElse(null);
        if (course == null) return Result.fail("课程不存在");
        if (enrollRepo.existsByUserAndCourseId(user, courseId)) return Result.fail("已报名该课程");
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        return Result.ok(enrollRepo.save(enrollment));
    }

    @GetMapping("/enrollments/my")
    public Result<List<CourseEnrollment>> myEnrollments(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        return Result.ok(enrollRepo.findByUser(user));
    }

    @PutMapping("/enrollments/{id}/progress")
    public Result<?> updateProgress(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        CourseEnrollment enrollment = enrollRepo.findById(id).orElse(null);
        if (enrollment == null) return Result.fail("报名记录不存在");

        if (body.containsKey("progress")) {
            int progress = ((Number) body.get("progress")).intValue();
            enrollment.setProgress(Math.min(100, Math.max(0, progress)));
        }
        if (body.containsKey("studyHours")) {
            double hours = ((Number) body.get("studyHours")).doubleValue();
            enrollment.setStudyHours(enrollment.getStudyHours() + hours);
        }
        enrollment.setLastStudyTime(LocalDateTime.now());

        if (enrollment.getProgress() >= 100) {
            enrollment.setProgress(100);
            enrollment.setStatus("COMPLETED");
        }

        enrollRepo.save(enrollment);
        return Result.ok(enrollment);
    }
}
