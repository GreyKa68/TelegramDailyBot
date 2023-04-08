package com.example.telegramdailybot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "telegrambot")
public class TelegramDailyBotProperties {
    private String botUsername;
    private String botToken;
}
