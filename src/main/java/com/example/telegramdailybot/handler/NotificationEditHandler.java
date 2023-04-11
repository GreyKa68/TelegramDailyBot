package com.example.telegramdailybot.handler;

import com.example.telegramdailybot.TelegramDailyBotInterface;
import com.example.telegramdailybot.model.Chat;
import com.example.telegramdailybot.model.Notification;
import com.example.telegramdailybot.model.ParseResult;
import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.repository.ChatRepository;
import com.example.telegramdailybot.repository.NotificationRepository;
import com.example.telegramdailybot.util.NotificationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NotificationEditHandler implements TelegramDailyBotInterface {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ChatRepository chatRepository;


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
        ParseResult parseResult = NotificationUtils.parseNotificationText(text);
        if (parseResult.hasError()) {
            return createErrorMessage(chatId, "Ошибка при парсинге уведомления. " + parseResult.getErrorMessage());
        }
        Notification notificationUpdated = parseResult.getNotification();

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

        Optional<Chat> optionalChat = chatRepository.findById(chatId);
        if (optionalChat.isPresent()) {
            Chat chatTemp = optionalChat.get();

            boolean isAdmin;
            if (chatTemp.getRole() != null) {
                isAdmin = chatTemp.getRole().equals("admin");
            } else {
                isAdmin = false;
            }

            Notification notificationCurrent = notificationRepository.findById(id).orElse(null);

            if (notificationCurrent != null && (isAdmin || notificationCurrent.getChatid().equals(chatId))) {
                notificationCurrent.setText(notificationUpdated.getText());
                notificationCurrent.setDatetime(notificationUpdated.getDatetime());
                notificationCurrent.setRepetition(notificationUpdated.getRepetition());
                notificationCurrent.setDatetimexcluded(notificationUpdated.getDatetimexcluded());
                // Save the notification to the database
                notificationRepository.save(notificationCurrent);
            }
        }
    }

    private SendMessage createSuccessMessage(Long chatId, String messageText) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText(messageText);
        return msg;
    }

}

