package com.grash.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GrashServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrashServerApplication.class, args);
    }
}
