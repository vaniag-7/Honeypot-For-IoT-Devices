package com.iot.honeypot.controller;

import com.iot.honeypot.honeypot.HttpHoneypot;
import com.iot.honeypot.honeypot.TelnetHoneypot;
import com.iot.honeypot.service.HoneypotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "*")
public class ServiceController {
    
    @Autowired
    private HoneypotService honeypotService;
    
    @Autowired
    private TelnetHoneypot telnetHoneypot;
    
    @Autowired
    private HttpHoneypot httpHoneypot;
    
    @GetMapping("/status")
    public Map<String, Object> getServiceStatus() {
        return honeypotService.getServiceStatus();
    }
    
    @PostMapping("/{serviceName}/toggle")
    public Map<String, Object> toggleService(@PathVariable String serviceName) {
        boolean newStatus = honeypotService.toggleService(serviceName);
        
        // Actually start/stop the service
        switch (serviceName.toLowerCase()) {
            case "telnet":
                if (newStatus) telnetHoneypot.start();
                else telnetHoneypot.stop();
                break;
            case "http":
                if (newStatus) httpHoneypot.start();
                else httpHoneypot.stop();
                break;
        }
        
        return Map.of(
            "service", serviceName,
            "running", newStatus,
            "message", serviceName + " service " + (newStatus ? "started" : "stopped")
        );
    }
    
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "✅ Backend is healthy and running!");
    }
}