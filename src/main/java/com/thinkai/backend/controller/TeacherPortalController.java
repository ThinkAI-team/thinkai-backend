package com.thinkai.backend.controller;

import com.thinkai.backend.security.TeacherOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teacher-portal")
@RequiredArgsConstructor
public class TeacherPortalController {

    @TeacherOnly
    @GetMapping("/dashboard")
    public ResponseEntity<String> getDashboard() {
        // Chỉ Teacher mới vào được Teacher Portal
        return ResponseEntity.ok("Teacher Portal Dashboard");
    }
}
