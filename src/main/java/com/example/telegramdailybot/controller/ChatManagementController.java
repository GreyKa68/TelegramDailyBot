package com.example.telegramdailybot.controller;

import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.service.ChatService;
import com.example.telegramdailybot.util.BotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;


@Controller
public class ChatManagementController {
    private final ChatService chatService;

    @Autowired
    public ChatManagementController(ChatService chatService) {
        this.chatService = chatService;
    }

    public SendMessage editChatsMessage(Update update) {
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

    public SendMessage addChats(Update update, Map<Long, UserActionState> userActionStates) {
        String text = update.getMessage().getText();
        chatService.addChatsFromText(text);

        // Remove the user from the userAddingStates map
        userActionStates.remove(update.getMessage().getFrom().getId());

        // Send a confirmation message to the user
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText("Чаты успешно добавлены");

        return message;
    }

    public SendMessage deleteChats(Update update, Map<Long, UserActionState> userActionStates) {
        String text = update.getMessage().getText();
        chatService.deleteChatsFromText(text);

        // Remove the user from the userAddingStates map
        userActionStates.remove(update.getMessage().getFrom().getId());

        // Send a confirmation message to the user
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText("Чаты успешно удалены");

        return message;
    }

    public SendMessage editChats(Update update, Map<Long, UserActionState> userActionStates) {
        String text = update.getMessage().getText();
        chatService.editChatsFromText(text);

        // Remove the user from the userAddingStates map
        userActionStates.remove(update.getMessage().getFrom().getId());

        // Send a confirmation message to the user
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText("Чаты успешно отредактированы");

        return message;
    }

}
