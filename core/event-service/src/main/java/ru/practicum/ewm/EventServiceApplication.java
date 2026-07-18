package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.practicum.ewm.interaction.client.CommentServiceClient;
import ru.practicum.ewm.interaction.client.RequestServiceClient;
import ru.practicum.ewm.interaction.client.UserServiceClient;

@EnableFeignClients(
        clients = {
                UserServiceClient.class,
                RequestServiceClient.class,
                CommentServiceClient.class
        }
)
@SpringBootApplication(
        scanBasePackages = {
                "ru.practicum.ewm",
                "ru.practicum.stat"
        }
)
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                EventServiceApplication.class,
                args
        );
    }
}