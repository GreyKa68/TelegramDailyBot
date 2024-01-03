package com.example.telegramdailybot.utils;

import com.example.telegramdailybot.model.ParseResult;
import com.example.telegramdailybot.util.BotUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.ZoneId;

public class BotUtilsTest {

    @Test
    public void testParseNotificationText_ValidInput() {
        // Arrange
        String input = "Текст уведомления: Test\nДата и время: 2022-12-31T23:59\nЧастота: Daily\nИсключения: - Исключить СБ и ВС";
        ZoneId timeZone = ZoneId.systemDefault();

        // Act
        ParseResult result = BotUtils.parseNotificationText(input, timeZone);

        // Assert
        assertNotNull(result.getNotification(), "Notification should not be null for valid input");
        assertNull(result.getErrorMessage(), "Error should be null for valid input");
        assertEquals("Test", result.getNotification().getText(), "Notification text should match input");
        // Add more assertions for other fields of the Notification object as needed
    }

    @Test
    public void testParseNotificationText_InvalidInput() {
        // Arrange
        String input = "Invalid input";
        ZoneId timeZone = ZoneId.systemDefault();

        // Act
        ParseResult result = BotUtils.parseNotificationText(input, timeZone);

        // Assert
        assertNull(result.getNotification(), "Notification should be null for invalid input");
        assertNotNull(result.getErrorMessage(), "Error should not be null for invalid input");
    }
}
