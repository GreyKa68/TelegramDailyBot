package com.example.telegramdailybot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

@Component
public class UserDeletionHandler implements TelegramDailyBotInterface {

    @Autowired
    private UserRepository userRepository;

    @Override
    public SendMessage handleUserEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleNotificationDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleNotificationEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
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
        String[] lines = text.split("\\n");

        for (String line : lines) {
            userRepository.findById(Integer.parseInt(line)).ifPresent(user ->
                    userRepository.delete(user)
            );
        }

        // Remove the user from the userDeletingStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Users have been deleted successfully.");

        return msg;
    }

}