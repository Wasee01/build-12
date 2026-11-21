package com.example.auditing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class AuditingApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditingApplication.class, args);
    }
}
