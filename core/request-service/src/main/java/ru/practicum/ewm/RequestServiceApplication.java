package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.stat.client.CollectorClient;

@EnableFeignClients
@SpringBootApplication
@Import(CollectorClient.class)
public class RequestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                RequestServiceApplication.class,
                args
        );
    }
}