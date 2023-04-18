package com.example.telegramdailybot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Data
@Component
@ConfigurationProperties(prefix = "telegrambot")
public class TelegramDailyBotProperties {
    private String botUsername;
    private String botToken;
    private ZoneId timeZone;

    public void setTimeZone(String timeZone) {
        this.timeZone = ZoneId.of(timeZone);
    }
}
