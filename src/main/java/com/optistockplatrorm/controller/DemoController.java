package com.optistockplatrorm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class DemoController {

    @GetMapping("/demo")
    public ResponseEntity<String> adminOnly() {
        return ResponseEntity.ok("Hello Admin - Rak connecti!");
    }
}