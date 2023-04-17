package com.example.telegramdailybot.controller;

import com.example.telegramdailybot.service.ChatService;
import com.example.telegramdailybot.util.BotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;


@Controller
public class ChatManagementController {
    private final ChatService chatService;

    @Autowired
    public ChatManagementController(ChatService chatService) {
        this.chatService = chatService;
    }

    public SendMessage editChats(Update update) {
        long chatId = update.getMessage().getChatId();
        boolean isUserChat = update.getMessage().getChat().isUserChat();
        boolean isAdmin = chatService.isAdmin(update.getMessage().getFrom().getId());

        if (isUserChat) {
            if (isAdmin) {
                String chatList = chatService.generateChatList();
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(chatList);
                message.setReplyMarkup(BotUtils.createInlineKeyboardMarkup("add", "delete", "edit"));
                return message;
            } else {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText("У вас нет прав администратора для редактирования чата!");
                return message;
            }
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Команда /editchats доступна только в приватных чатах!");
            return message;
        }
    }

}
