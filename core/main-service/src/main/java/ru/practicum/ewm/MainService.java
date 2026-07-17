package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.ewm", "ru.practicum.stat"})
public class MainService {
    public static void main(String[] args) {
        SpringApplication.run(MainService.class, args);
    }
}