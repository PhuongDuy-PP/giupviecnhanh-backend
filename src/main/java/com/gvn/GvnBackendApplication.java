package com.gvn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class GvnBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GvnBackendApplication.class, args);
    }
}

