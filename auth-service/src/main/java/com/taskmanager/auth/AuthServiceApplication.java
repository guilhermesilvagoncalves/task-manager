package com.taskmanager.auth;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// Relative server: the paths resolve against wherever the document was fetched from (gateway or service).
@OpenAPIDefinition(info = @Info(title = "auth-service", version = "v1"), servers = @Server(url = "/"))
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
