package com.quickbite.auth.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @GetMapping("/")
    public ResponseEntity<String> testingAPI(HttpServletRequest request){
        System.out.println("Testing API working for token "+request.getHeader("Authorization"));
        return ResponseEntity.ok("JWT Filter working properly");
    }
}
