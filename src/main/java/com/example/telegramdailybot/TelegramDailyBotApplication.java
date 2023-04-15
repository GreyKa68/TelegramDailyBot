package com.example.telegramdailybot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {
        "com.example.telegramdailybot",
        "org.telegram.telegrambots"
})
public class TelegramDailyBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramDailyBotApplication.class, args);
    }

}
