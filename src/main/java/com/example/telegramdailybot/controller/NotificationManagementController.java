package com.example.telegramdailybot.controller;

import com.example.telegramdailybot.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class NotificationManagementController {
    private final NotificationService notificationService;

    @Autowired
    public NotificationManagementController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public String showNotifications(Update update) {
        List<String> fieldsToDisplay = Arrays.asList("text", "datetime", "repetition", "datetimexcluded");
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("text", "Текст уведомления: ");
        customHeaders.put("datetime", "Дата и время: ");
        customHeaders.put("repetition", "Частота: ");
        customHeaders.put("datetimexcluded", "Исключения: \n");
        return notificationService.generateNotificationListMessage(update.getMessage().getChatId(), fieldsToDisplay, customHeaders);
    }
}
