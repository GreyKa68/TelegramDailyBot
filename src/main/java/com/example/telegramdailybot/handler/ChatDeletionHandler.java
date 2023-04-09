package com.example.telegramdailybot.handler;

import com.example.telegramdailybot.TelegramDailyBotInterface;
import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

@Component
public class ChatDeletionHandler implements TelegramDailyBotInterface {

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
    public SendMessage handleNotificationDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleChatEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }


    @Override
    public SendMessage handleChatDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        // Your handleChatDeleting implementation
        String[] lines = text.split("\\n");

        for (String line : lines) {
            chatRepository.findById(Long.parseLong(line)).ifPresent(chat ->
                    chatRepository.delete(chat)
            );
        }

        // Remove the user from the userActionStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Чаты успешно удалены");

        return msg;
    }
}
