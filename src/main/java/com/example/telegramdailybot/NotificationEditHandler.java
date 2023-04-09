package com.example.telegramdailybot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NotificationEditHandler implements TelegramDailyBotInterface {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public SendMessage handleUserEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleUserDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleNotificationDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleChatEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleChatDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Transactional
    @Override
    public SendMessage handleNotificationEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        // Extract the ID from the text
        Integer id = extractNotificationId(text);
        if (id == null) {
            return createErrorMessage(chatId, "Ошибка при парсинге ID. Пожалуйста, проверьте формат и попробуйте еще раз.");
        }

        // Parse the notification from the message text
        Notification notificationUpdated = NotificationUtils.parseNotificationText(text);
        if (notificationUpdated == null) {
            return createErrorMessage(chatId, "Ошибка при парсинге уведомления. Пожалуйста, проверьте формат и попробуйте еще раз.");
        }

        // Update the notification in the database
        updateNotificationInDatabase(chatId, id, notificationUpdated);

        // Remove the user from the UserActionStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        return createSuccessMessage(chatId, "Уведомление успешно изменено");
    }

    private Integer extractNotificationId(String text) {
        String regex = "ID:\\s*(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private SendMessage createErrorMessage(Long chatId, String messageText) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(messageText);
        return msg;
    }

    private void updateNotificationInDatabase(Long chatId, Integer id, Notification notificationUpdated) {
        notificationUpdated.setChatid(chatId);

        Notification notificationCurrent = notificationRepository.findById(id).orElse(null);

        if (notificationCurrent != null) {
            notificationCurrent.setText(notificationUpdated.getText());
            notificationCurrent.setDatetime(notificationUpdated.getDatetime());
            notificationCurrent.setRepetition(notificationUpdated.getRepetition());
            notificationCurrent.setChatid(notificationUpdated.getChatid());
            notificationCurrent.setDatetimexcluded(notificationUpdated.getDatetimexcluded());
            // Save the notification to the database
            notificationRepository.save(notificationCurrent);
        }
    }

    private SendMessage createSuccessMessage(Long chatId, String messageText) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(messageText);
        return msg;
    }

}

