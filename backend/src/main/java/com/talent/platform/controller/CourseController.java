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

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CourseController {

    private final CourseRepository courseRepo;
    private final CourseEnrollmentRepository enrollRepo;
    private final UserRepository userRepo;

    @GetMapping("/api/courses")
    public Result<List<Course>> list() {
        return Result.ok(courseRepo.findAll());
    }

    @GetMapping("/api/courses/{id}")
    public Result<Course> getById(@PathVariable Long id) {
        return courseRepo.findById(id).map(Result::ok).orElse(Result.fail("课程不存在"));
    }

    @PostMapping("/api/courses")
    public Result<Course> create(@RequestBody Course course) {
        return Result.ok(courseRepo.save(course));
    }

    @PutMapping("/api/courses/{id}")
    public Result<Course> update(@PathVariable Long id, @RequestBody Course updated) {
        Course course = courseRepo.findById(id).orElse(null);
        if (course == null) return Result.fail("课程不存在");
        course.setTitle(updated.getTitle());
        course.setDescription(updated.getDescription());
        course.setTeacher(updated.getTeacher());
        course.setCategory(updated.getCategory());
        course.setCoverUrl(updated.getCoverUrl());
        return Result.ok(courseRepo.save(course));
    }

    @DeleteMapping("/api/courses/{id}")
    public Result<?> delete(@PathVariable Long id) {
        courseRepo.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/api/enrollments")
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

    @GetMapping("/api/enrollments/my")
    public Result<List<CourseEnrollment>> myEnrollments(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepo.findByUsername(userDetails.getUsername()).orElseThrow();
        return Result.ok(enrollRepo.findByUser(user));
    }
}
