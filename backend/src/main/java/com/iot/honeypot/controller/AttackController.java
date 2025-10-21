package com.iot.honeypot.controller;

import com.iot.honeypot.entity.AttackLog;
import com.iot.honeypot.service.AttackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attacks")
@CrossOrigin(origins = "*")
public class AttackController {
    
    @Autowired
    private AttackService attackService;
    
    @GetMapping
    public List<AttackLog> getAllAttacks() {
        return attackService.getAllAttacks();
    }
    
    @GetMapping("/recent")
    public List<AttackLog> getRecentAttacks(@RequestParam(defaultValue = "10") int count) {
        return attackService.getRecentAttacks(count);
    }
    
    @GetMapping("/type/{serviceType}")
    public List<AttackLog> getAttacksByType(@PathVariable String serviceType) {
        return attackService.getAttacksByType(serviceType);
    }
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return attackService.getStats();
    }
    
    @PostMapping("/log")
    public String logAttack(@RequestBody AttackLog attack) {
        attackService.logAttack(attack);
        return "Attack logged successfully!";
    }
    
    @DeleteMapping("/clear")
    public String clearAttacks() {
        attackService.clearAllAttacks();
        return "All attacks cleared!";
    }
}