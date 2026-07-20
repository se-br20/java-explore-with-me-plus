package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.practicum.ewm.comments.client.EventServiceClient;
import ru.practicum.ewm.comments.client.UserServiceClient;

@EnableFeignClients(
        clients = {
                UserServiceClient.class,
                EventServiceClient.class
        }
)
@SpringBootApplication
public class CommentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                CommentServiceApplication.class,
                args
        );
    }
}