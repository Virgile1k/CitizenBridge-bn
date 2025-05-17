package com.citizenbridge.citizenbridge.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple REST controller for testing endpoints
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * Simple GET endpoint that returns a test message
     * @return A test response message
     */
    @GetMapping("/hello")
    public String helloTest() {
        return "Hello from TestController!";
    }

    /**
     * Health check endpoint
     * @return Status message
     */
    @GetMapping("/status")
    public String getStatus() {
        return "Service is up and running!";
    }
}