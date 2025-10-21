package com.iot.honeypot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HoneypotApplication {
    public static void main(String[] args) {
        SpringApplication.run(HoneypotApplication.class, args);
        System.out.println("IOT HONEYPOT BACKEND STARTED!");
        System.out.println("API: http://localhost:8080");
        System.out.println("Open: http://localhost:8080/api/test");
    }
}