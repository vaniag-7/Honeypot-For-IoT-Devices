package com.iot.honeypot.service;

import com.iot.honeypot.entity.AttackLog;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class AttackService {
    private List<AttackLog> attackLogs = new CopyOnWriteArrayList<>();
    
    @PostConstruct
    public void init() {
        // Add sample attacks for demo
        attackLogs.add(new AttackLog("192.168.1.100", "TELNET", "Login: admin/admin123", "TP-Link Router"));
        attackLogs.add(new AttackLog("10.0.0.55", "HTTP", "GET /admin/login.php", "D-Link Camera"));
        attackLogs.add(new AttackLog("172.16.23.45", "TELNET", "Login: root/12345", "Cisco Switch"));
        attackLogs.add(new AttackLog("203.0.113.12", "HTTP", "POST /cgi-bin/login.cgi", "Netgear Router"));
        
        System.out.println("📊 Loaded " + attackLogs.size() + " sample attacks for demo");
    }
    
    public void logAttack(AttackLog attack) {
        attackLogs.add(0, attack); // Add to beginning for recent first
        System.out.println("🚨 NEW ATTACK: " + attack.getSourceIp() + " -> " + attack.getServiceType());
    }
    
    public List<AttackLog> getAllAttacks() {
        return new ArrayList<>(attackLogs);
    }
    
    public List<AttackLog> getRecentAttacks(int count) {
        return attackLogs.stream()
                .limit(count)
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAttacks", attackLogs.size());
        stats.put("lastAttack", attackLogs.isEmpty() ? "Never" : 
            attackLogs.get(0).getTimestamp().toString());
        
        // Attacks by service type
        Map<String, Long> byService = attackLogs.stream()
            .collect(Collectors.groupingBy(AttackLog::getServiceType, Collectors.counting()));
        stats.put("attacksByService", byService);
        
        // Attacks by device
        Map<String, Long> byDevice = attackLogs.stream()
            .collect(Collectors.groupingBy(AttackLog::getDeviceEmulated, Collectors.counting()));
        stats.put("attacksByDevice", byDevice);
        
        // Top attackers
        Map<String, Long> topAttackers = attackLogs.stream()
            .collect(Collectors.groupingBy(AttackLog::getSourceIp, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        stats.put("topAttackers", topAttackers);
        
        return stats;
    }
    
    public List<AttackLog> getAttacksByType(String serviceType) {
        return attackLogs.stream()
                .filter(attack -> serviceType.equalsIgnoreCase(attack.getServiceType()))
                .collect(Collectors.toList());
    }
    
    public void clearAllAttacks() {
        attackLogs.clear();
        System.out.println("🗑️ All attack logs cleared");
    }
}