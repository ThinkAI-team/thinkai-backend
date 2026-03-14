package com.thinkai.backend.controller;

import com.thinkai.backend.dto.ApiResponse;
import com.thinkai.backend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * GET /courses — Danh sách khóa học (Public)
     *
     * Query params: keyword, priceMin, priceMax, sortBy, sortDir, page, size
     * Response: { status, message, data: { content, page, size, totalElements, totalPages } }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublishedCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal priceMin,
            @RequestParam(required = false) BigDecimal priceMax,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Map<String, Object> data = courseService.getPublishedCourses(
                keyword, priceMin, priceMax, sortBy, sortDir, page, size
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
