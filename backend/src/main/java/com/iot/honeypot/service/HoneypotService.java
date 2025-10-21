package com.iot.honeypot.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class HoneypotService {
    private Map<String, Boolean> serviceStatus = new HashMap<>();
    
    public HoneypotService() {
        serviceStatus.put("telnet", true);
        serviceStatus.put("http", true);
        serviceStatus.put("ssh", false);
    }
    
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("telnet", Map.of(
            "running", serviceStatus.get("telnet"),
            "port", 2323,
            "description", "TP-Link Router Emulator"
        ));
        status.put("http", Map.of(
            "running", serviceStatus.get("http"),
            "port", 8081,
            "description", "D-Link Camera Web Interface"
        ));
        status.put("ssh", Map.of(
            "running", serviceStatus.get("ssh"),
            "port", 2222,
            "description", "SSH Server (Coming Soon)"
        ));
        return status;
    }
    
    public boolean toggleService(String serviceName) {
        if (serviceStatus.containsKey(serviceName)) {
            boolean current = serviceStatus.get(serviceName);
            serviceStatus.put(serviceName, !current);
            System.out.println("🔧 " + serviceName + " service " + 
                (current ? "stopped" : "started"));
            return !current;
        }
        return false;
    }
}