package com.iot.honeypot.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SimpleController {
    
    @GetMapping("/test")
    public String test() {
        return "🚀 IOT HONEYPOT BACKEND IS WORKING!";
    }
    
    @GetMapping("/attacks")
    public String getAttacks() {
        return """
               [
                 {
                   "id": 1,
                   "sourceIp": "192.168.1.100",
                   "serviceType": "TELNET",
                   "message": "Failed login attempt",
                   "timestamp": "2024-01-15T10:30:00"
                 },
                 {
                   "id": 2,
                   "sourceIp": "10.0.0.55",
                   "serviceType": "HTTP", 
                   "message": "Web interface access",
                   "timestamp": "2024-01-15T10:25:00"
                 }
               ]
               """;
    }
    
    @GetMapping("/stats")
    public String getStats() {
        return """
               {
                 "totalAttacks": 15,
                 "lastAttack": "2024-01-15T10:30:00",
                 "topService": "TELNET",
                 "topAttacker": "192.168.1.100"
               }
               """;
    }
}