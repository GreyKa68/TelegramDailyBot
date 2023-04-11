package com.example.telegramdailybot;

import com.example.telegramdailybot.model.UserActionState;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.ZoneId;
import java.util.Map;

public interface TelegramDailyBotInterface {
    SendMessage handleUserDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId);

    SendMessage handleUserEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId);

    SendMessage handleNotificationDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId);

    SendMessage handleNotificationEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId, ZoneId timeZone);

    SendMessage handleChatDeleting(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId);

    SendMessage handleChatEditing(Map<Long, UserActionState> userActionStates, Message message, String text, Long chatId, Long userId);
}

