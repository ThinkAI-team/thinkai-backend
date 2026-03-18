package com.thinkai.backend.controller;

import com.thinkai.backend.security.AdminOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin-panel")
@RequiredArgsConstructor
public class AdminPanelController {

    @AdminOnly
    @GetMapping("/stats")
    public ResponseEntity<String> getStats() {
        // Chỉ Admin mới vào được Admin Panel
        return ResponseEntity.ok("System Statistics");
    }
}
