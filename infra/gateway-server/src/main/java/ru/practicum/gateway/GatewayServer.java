package ru.practicum.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.practicum.gateway.startup.RequiredServicesStartupWaiter;

@SpringBootApplication
public class GatewayServer {

    public static void main(String[] args) {
        RequiredServicesStartupWaiter.waitIfEnabled();
        SpringApplication.run(GatewayServer.class, args);
    }
}