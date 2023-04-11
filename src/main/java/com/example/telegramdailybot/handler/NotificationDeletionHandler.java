package com.example.telegramdailybot.handler;

import com.example.telegramdailybot.TelegramDailyBotInterface;
import com.example.telegramdailybot.model.Chat;
import com.example.telegramdailybot.model.Notification;
import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.repository.ChatRepository;
import com.example.telegramdailybot.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;
import java.util.Optional;

@Component
public class NotificationDeletionHandler implements TelegramDailyBotInterface {

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
    public SendMessage handleNotificationEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleChatDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleChatEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Transactional
    @Override
    public SendMessage handleNotificationDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {

        Optional<Chat> optionalChat = chatRepository.findById(chatId);
        if (optionalChat.isPresent()) {
            Chat chatTemp = optionalChat.get();

            boolean isAdmin = false;
            if (chatTemp.getRole() != null) {
                isAdmin = chatTemp.getRole().equals("admin");
            } else {
                isAdmin = false;
            }

            String[] lines = text.split("\\n");

            for (String line : lines) {
                int notificationId = Integer.parseInt(line);
                Notification notification = notificationRepository.findById(notificationId).orElse(null);

                if (notification != null && (isAdmin || notification.getChatid().equals(chatId))) {
                    notificationRepository.deleteById(notificationId);
                }
            }
        }

        // Remove the user from the userDeletingStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Уведомления успешно удалены");

        return msg;
    }
}
