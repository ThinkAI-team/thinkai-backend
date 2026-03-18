package com.thinkai.backend.service;

import com.thinkai.backend.dto.MyCourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporary Mock CourseService to fix compilation issues in UserController.
 * Real implementation belongs to Course Catalog & Enrollment module.
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    public List<MyCourseResponse> getMyCourses(Long userId) {
        // Mock implementation to avoid breaking UserController
        return new ArrayList<>();
    }
}
