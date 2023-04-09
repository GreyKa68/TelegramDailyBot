package com.example.telegramdailybot.handler;

import com.example.telegramdailybot.TelegramDailyBotInterface;
import com.example.telegramdailybot.model.Chat;
import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Map;

@Component
public class ChatEditHandler implements TelegramDailyBotInterface {

    @Autowired
    private ChatRepository chatRepository;

    @Override
    public SendMessage handleUserDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

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
    public SendMessage handleChatDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        return null;
    }

    @Override
    public SendMessage handleChatEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId) {
        String[] lines = text.split("\\n");

        for (String line : lines) {
            String[] parts = line.split(",", 3);
            if (parts.length == 3) {
                Long telegramid = Long.parseLong(parts[0].trim());
                String name = parts[1].trim();
                String role = parts[2].trim();

                Chat chat = chatRepository.findById(telegramid).orElse(null);

                if (chat != null) {
                    chat.setName(name);
                    chat.setRole(role);
                    chatRepository.save(chat);
                }
            }
        }

        // Remove the user from the userActionStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Чаты успешно изменены");
        return msg;
    }
}

