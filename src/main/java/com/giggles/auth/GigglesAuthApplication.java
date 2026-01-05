package com.giggles.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GigglesAuthApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GigglesAuthApplication.class, args);
    }
}

