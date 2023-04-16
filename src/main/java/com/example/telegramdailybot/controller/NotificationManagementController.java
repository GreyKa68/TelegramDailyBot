package com.example.telegramdailybot.controller;

import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.service.ChatService;
import com.example.telegramdailybot.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

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

    public SendMessage editNotifications(Update update, Map<Long, UserActionState> userActionStates) {
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
            InlineKeyboardMarkup inlineKeyboardMarkup = createInlineKeyboardMarkup("add_notification", "delete_notifications", "edit_notification");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setReplyMarkup(inlineKeyboardMarkup);
            return message;
        }

    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(String addCallbackData, String deleteCallbackData, String editCallbackData) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton addButton = new InlineKeyboardButton("Add");
        addButton.setCallbackData(addCallbackData);

        InlineKeyboardButton deleteButton = new InlineKeyboardButton("Delete");
        deleteButton.setCallbackData(deleteCallbackData);

        InlineKeyboardButton editButton = new InlineKeyboardButton("Edit");
        editButton.setCallbackData(editCallbackData);

        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        keyboardButtonsRow.add(addButton);
        keyboardButtonsRow.add(deleteButton);
        keyboardButtonsRow.add(editButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow);

        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }
}
