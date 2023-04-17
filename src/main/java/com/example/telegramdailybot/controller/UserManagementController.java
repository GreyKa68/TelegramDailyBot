package com.example.telegramdailybot.controller;

import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.service.ChatService;
import com.example.telegramdailybot.service.UserService;
import com.example.telegramdailybot.util.BotUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
public class UserManagementController {

    private final UserService userService;
    private final ChatService chatService;

    @Autowired
    public UserManagementController(UserService userService, ChatService chatService) {
        this.userService = userService;
        this.chatService = chatService;
    }

    public String addUser(Update update) {
        return "User added successfully.";
    }

    public String deleteUser(Update update) {
        return "User deleted successfully.";
    }

    public SendMessage resetWinners(Update update) {
        userService.resetWinners(update.getMessage().getChatId());
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText("Winners reset successfully.");
        return message;
    }

    public SendMessage showUsers(Update update) {
        List<String> fieldsToDisplay = Arrays.asList("name", "username", "haswon");
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("name", "имя");
        customHeaders.put("haswon", "выиграл");
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText(userService.generateUserListMessage(update.getMessage().getChatId(), fieldsToDisplay, customHeaders));
        return message;
    }

    public SendMessage findWinner(Update update) {
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText(userService.findWinner(update.getMessage().getChatId()));
        return message;
    }

    public SendMessage editUsersMessage(Update update, Map<Long, UserActionState> userActionStates) {
        long chatId = update.getMessage().getChatId();
        boolean isUserChat = update.getMessage().getChat().isUserChat();
        boolean isAdmin = chatService.isAdmin(update.getMessage().getFrom().getId());
        if (isUserChat && isAdmin) {
            userActionStates.put(chatId, UserActionState.WAITING_FOR_CHAT_ID_TO_EDIT_USERS);
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Введите ID чата для редактирования пользователей:");
            return message;
        } else {
            List<String> fieldsToDisplay = Arrays.asList("id", "name", "username", "haswon");
            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("name", "имя");
            customHeaders.put("haswon", "выиграл");
            String text = userService.generateUserListMessage(chatId, fieldsToDisplay, customHeaders);
            text = text + "\n Выберите действие:";
            InlineKeyboardMarkup inlineKeyboardMarkup = BotUtils.createInlineKeyboardMarkup("add_users", "delete_users", "edit_users");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setReplyMarkup(inlineKeyboardMarkup);
            return message;
        }
    }

    @Transactional
    public SendMessage addUsers(Update update, Map<Long, UserActionState> userActionStates) {
        // Parse and add users from the message text
        String text = update.getMessage().getText();
        userService.addUsersFromText(text, update.getMessage().getChatId());

        // Remove the user from the userAddingStates map
        userActionStates.remove(update.getMessage().getFrom().getId());

        // Send a confirmation message to the user
        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText("Участники успешно добавлены");

        return message;
    }

    public SendMessage deleteUsers(Update update, Map<Long, UserActionState> userActionStates) {
        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        userService.deleteUsersFromText(text, chatId, userId);

        // Remove the user from the userDeletingStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Участники успешно удалены");

        return message;
    }

    public SendMessage editUsers(Update update, Map<Long, UserActionState> userActionStates) {
        String text = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        userService.editUsersFromText(text, chatId, userId);

        // Remove the user from the userDeletingStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Участники успешно отредактированы");

        return message;
    }

    public SendMessage editUsersAdmin(Update update, Map<Long, UserActionState> userActionStates) {
        long chatId = update.getMessage().getChatId();
        long userId = update.getMessage().getFrom().getId();

        try {
            Long targetChatId = Long.parseLong(update.getMessage().getText());
            userActionStates.remove(userId);

            List<String> fieldsToDisplay = Arrays.asList("id", "name", "username", "haswon");
            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("name", "имя");
            customHeaders.put("haswon", "выиграл");
            String text = userService.generateUserListMessage(targetChatId, fieldsToDisplay, customHeaders);
            text = text + "\n Выберите действие:";
            InlineKeyboardMarkup inlineKeyboardMarkup = BotUtils.createInlineKeyboardMarkup("add_users", "delete_users", "edit_users");

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setReplyMarkup(inlineKeyboardMarkup);
            return message;

        } catch (NumberFormatException e) {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText("Неверный формат ID чата. Введите корректный ID чата:");
            return message;
        }
    }

}

