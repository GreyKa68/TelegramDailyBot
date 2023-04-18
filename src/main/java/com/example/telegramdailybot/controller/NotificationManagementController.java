package com.example.telegramdailybot.controller;

import com.example.telegramdailybot.model.Notification;
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

    public SendMessage addNotificationByAdmin(Update update, Map<Long, UserActionState> userActionStates) {
        String[] parts = update.getMessage().getText().split("\n", 2);
        try {
            long targetChatId = Long.parseLong(parts[0]);

            String textMessage = notificationService.addNotificationFromText(parts[1], targetChatId);

            // Remove the user from the userAddingStates map
            userActionStates.remove(update.getMessage().getFrom().getId());

            SendMessage message = new SendMessage();
            message.setChatId(update.getMessage().getChatId());
            message.setText(textMessage);
            return message;
        } catch (NumberFormatException e) {
            SendMessage message = new SendMessage();
            message.setChatId(update.getMessage().getChatId());
            message.setText("Неверный формат ID чата. Введите корректный ID чата:");
            return message;
        }
    }

    public SendMessage initiateAddNotificationProcess(Update update, Map<Long, UserActionState> userActionStates) {
        boolean isUserChat = update.getCallbackQuery().getMessage().getChat().isUserChat();
        boolean isAdmin = chatService.isAdmin(update.getCallbackQuery().getFrom().getId());
        if (isUserChat && isAdmin) {
            userActionStates.put(update.getCallbackQuery().getFrom().getId(), UserActionState.WAITING_FOR_CHAT_ID_TO_ADD_NOTIFICATION);
            String text = """
                    Первой строкой вышлите ID чата, в который хотите добавить уведомления. Далее пришлите уведомление согласно следующему шаблону. Для удобства шаблон можно скопировать, вставить и отредактировать
                                    
                    -1234567890
                    Текст уведомления: Все на дейли, сегодня шарит @name, @username!
                    Дата и время: 2023-04-06T14:00
                    Частота: {once|minutely|hourly|daily|weekly|monthly|yearly}
                    Исключения:
                      - Исключить СБ и ВС
                      - Исключить дни:
                        * 2023-04-12 (every 7 days)
                        * 2023-04-24 (every 21 days)
                        * 2023-04-07 (every 7 days)""";
            SendMessage message = new SendMessage();
            message.setChatId(update.getCallbackQuery().getMessage().getChatId());
            message.setText(text);
            return message;
        } else {
            userActionStates.put(update.getCallbackQuery().getFrom().getId(), UserActionState.WAITING_FOR_NOTIFICATION_TO_ADD);

            String text = """
                    Пожалуйста, пришлите уведомление согласно следующему шаблону. Для удобства шаблон можно скопировать, вставить и отредактировать

                    Текст уведомления: Все на дейли, сегодня шарит @name, @username!
                    Дата и время: 2023-04-06T14:00
                    Частота: {once|minutely|hourly|daily|weekly|monthly|yearly}
                    Исключения:
                      - Исключить СБ и ВС
                      - Исключить дни:
                        * 2023-04-12 (every 7 days)
                        * 2023-04-24 (every 21 days)
                        * 2023-04-07 (every 7 days)""";

            SendMessage message = new SendMessage();
            message.setChatId(update.getCallbackQuery().getMessage().getChatId());
            message.setText(text);
            return message;
        }
    }

    public SendMessage initiateDeleteNotificationsProcess(Update update, Map<Long, UserActionState> userActionStates) {
        userActionStates.put(update.getCallbackQuery().getFrom().getId(), UserActionState.WAITING_FOR_NOTIFICATION_TO_DELETE);
        String text = """
                Пожалуйста, вышлите ID уведомлений, которые вы хотите удалить, каждый ID с новой строчки. Например:

                10
                11
                12""";
        SendMessage message = new SendMessage();
        message.setChatId(update.getCallbackQuery().getMessage().getChatId());
        message.setText(text);
        return message;
    }

    public SendMessage initiateEditNotificationProcess(Update update, Map<Long, UserActionState> userActionStates) {
        userActionStates.put(update.getCallbackQuery().getFrom().getId(), UserActionState.WAITING_FOR_NOTIFICATION_TO_EDIT);
        String text = """
                Пожалуйста, пришлите измененное уведомление согласно следующему шаблону. Для удобства скопируйте предыдущую версию уведомления и измените ее
                                
                ID: 11
                Текст уведомления: Все на дейли, сегодня шарит @name, @username!
                Дата и время: 2023-04-06T14:00
                Частота: {once|minutely|hourly|daily|weekly|monthly|yearly}
                Исключения:
                  - Исключить СБ и ВС
                  - Исключить дни:
                    * 2023-04-12 (every 7 days)
                    * 2023-04-24 (every 21 days)
                    * 2023-04-07 (every 7 days)""";
        SendMessage message = new SendMessage();
        message.setChatId(update.getCallbackQuery().getMessage().getChatId());
        message.setText(text);
        return message;
    }

    public Notification save(Notification notification) {
        return notificationService.save(notification);
    }

    public List<Notification> findAll() {
        return notificationService.findAll();
    }

    public void delete(Notification notification) {
        notificationService.delete(notification);
    }
}
