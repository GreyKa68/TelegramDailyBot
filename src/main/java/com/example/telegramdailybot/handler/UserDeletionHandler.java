package com.example.telegramdailybot.handler;

import com.example.telegramdailybot.TelegramDailyBotInterface;
import com.example.telegramdailybot.model.Chat;
import com.example.telegramdailybot.model.User;
import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.repository.ChatRepository;
import com.example.telegramdailybot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@Component
public class UserDeletionHandler implements TelegramDailyBotInterface {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Override
    public SendMessage handleUserEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleNotificationDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleNotificationEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId, ZoneId timeZone) {
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
    public SendMessage handleUserDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        // Your handleUserDeleting implementation

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
                int userIdtoDelete = Integer.parseInt(line);
                User user = userRepository.findById(userIdtoDelete).orElse(null);

                if (user != null && (isAdmin || user.getChatid().equals(chatId))) {
                    userRepository.deleteById(userIdtoDelete);
                }
            }
        }

        // Remove the user from the userDeletingStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Участники успешно удалены");

        return msg;
    }

}
