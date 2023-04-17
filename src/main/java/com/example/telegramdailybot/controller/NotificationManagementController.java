package com.example.telegramdailybot.controller;

import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.service.ChatService;
import com.example.telegramdailybot.service.NotificationService;
import com.example.telegramdailybot.util.BotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class NotificationManagementController {
    private final NotificationService notificationService;
    private final ChatService chatService;

    @Autowired
    public NotificationManagementController(NotificationService notificationService, ChatService chatService) {
        this.notificationService = notificationService;
        this.chatService = chatService;
    }

    public SendMessage showNotifications(Update update) {
        List<String> fieldsToDisplay = Arrays.asList("text", "datetime", "repetition", "datetimexcluded");
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("text", "Текст уведомления: ");
        customHeaders.put("datetime", "Дата и время: ");
        customHeaders.put("repetition", "Частота: ");
        customHeaders.put("datetimexcluded", "Исключения: \n");
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText(notificationService.generateNotificationListMessage(update.getMessage().getChatId(), fieldsToDisplay, customHeaders));
        return message;
    }

    public SendMessage editNotificationsMessage(Update update, Map<Long, UserActionState> userActionStates) {
        long chatId = update.getMessage().getChatId();
        boolean isUserChat = update.getMessage().getChat().isUserChat();
        boolean isAdmin = chatService.isAdmin(update.getMessage().getFrom().getId());
        if (isUserChat && isAdmin) {
            userActionStates.put(chatId, UserActionState.WAITING_FOR_CHAT_ID_TO_EDIT_NOTIFICATIONS);
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Введите ID чата для редактирования уведомлений:");
            return message;
        } else {
            List<String> fieldsToDisplay = Arrays.asList("id", "text", "datetime", "repetition", "datetimexcluded");
            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("id", "ID: ");
            customHeaders.put("text", "Текст уведомления: ");
            customHeaders.put("datetime", "Дата и время: ");
            customHeaders.put("repetition", "Частота: ");
            customHeaders.put("datetimexcluded", "Исключения: \n");
            String text = notificationService.generateNotificationListMessage(chatId, fieldsToDisplay, customHeaders);
            text = text + "\n Выберите действие:";

            // Create an inline keyboard markup for editing Notifications.
            InlineKeyboardMarkup inlineKeyboardMarkup = BotUtils.createInlineKeyboardMarkup("add_notification", "delete_notifications", "edit_notification");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setReplyMarkup(inlineKeyboardMarkup);
            return message;
        }

    }

    public SendMessage addNotification(Update update, Map<Long, UserActionState> userActionStates) {
        String text = notificationService.addNotificationFromText(update.getMessage().getText(), update.getMessage().getChatId());

        // Remove the user from the userAddingStates map
        userActionStates.remove(update.getMessage().getFrom().getId());

        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText(text);
        return message;
    }

    public SendMessage deleteNotifications(Update update, Map<Long, UserActionState> userActionStates) {
        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        notificationService.deleteNotificationsFromText(text, chatId, userId);

        // Remove the user from the userAddingStates map
        userActionStates.remove(userId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Уведомления успешно удалены");
        return message;
    }

    public SendMessage editNotification(Update update, Map<Long, UserActionState> userActionStates) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        String text = notificationService.editNotificationFromText(update.getMessage().getText(), chatId, userId);

        // Remove the user from the userAddingStates map
        userActionStates.remove(userId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        return message;
    }

    public SendMessage editNotificationsByAdmin(Update update, Map<Long, UserActionState> userActionStates) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        try {
            Long targetChatId = Long.parseLong(update.getMessage().getText());
            userActionStates.remove(userId);

            List<String> fieldsToDisplay = Arrays.asList("id", "text", "datetime", "repetition", "datetimexcluded");
            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("id", "ID: ");
            customHeaders.put("text", "Текст уведомления: ");
            customHeaders.put("datetime", "Дата и время: ");
            customHeaders.put("repetition", "Частота: ");
            customHeaders.put("datetimexcluded", "Исключения: \n");
            String text = notificationService.generateNotificationListMessage(targetChatId, fieldsToDisplay, customHeaders);
            text = text + "\n Выберите действие:";

            // Create an inline keyboard markup for editing Notifications.
            InlineKeyboardMarkup inlineKeyboardMarkup = BotUtils.createInlineKeyboardMarkup("add_notification", "delete_notifications", "edit_notification");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setReplyMarkup(inlineKeyboardMarkup);
            return message;
        } catch (NumberFormatException e) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Неверный формат ID чата. Введите корректный ID чата:");
            return message;
        }
    }
}
