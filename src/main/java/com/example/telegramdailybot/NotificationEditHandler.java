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
        // Parse the notification from the message text
        String regex = "ID:\\s*(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            // Send an error message if the text could not be parsed
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("Error parsing the notification ID. Please check the format and try again.");
            return msg;
        }
        Integer id = Integer.parseInt(matcher.group(1));
        Notification notificationUpdated = NotificationUtils.parseNotificationText(text);
        if (notificationUpdated == null) {
            // Send an error message if the text could not be parsed
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("Error parsing the notification text. Please check the format and try again.");
            return msg;
        }
        // Set the chat ID
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

        // Remove the user from the UserActionStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Notification has been edited successfully.");

        return msg;
    }
}

