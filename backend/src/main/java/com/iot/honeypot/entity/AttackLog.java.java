package com.iot.honeypot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attack_logs")
public class AttackLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String sourceIp;
    private String serviceType;
    private String payload;
    private String usernameAttempt;
    private String passwordAttempt;
    private LocalDateTime timestamp;
    private String deviceEmulated;
    
    public AttackLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public AttackLog(String sourceIp, String serviceType, String payload, String deviceEmulated) {
        this();
        this.sourceIp = sourceIp;
        this.serviceType = serviceType;
        this.payload = payload;
        this.deviceEmulated = deviceEmulated;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    
    public String getUsernameAttempt() { return usernameAttempt; }
    public void setUsernameAttempt(String usernameAttempt) { this.usernameAttempt = usernameAttempt; }
    
    public String getPasswordAttempt() { return passwordAttempt; }
    public void setPasswordAttempt(String passwordAttempt) { this.passwordAttempt = passwordAttempt; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getDeviceEmulated() { return deviceEmulated; }
    public void setDeviceEmulated(String deviceEmulated) { this.deviceEmulated = deviceEmulated; }
}