package com.example.telegramdailybot.controller;

import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.service.ChatService;
import com.example.telegramdailybot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

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

    public SendMessage editUsers(Update update, Map<Long, UserActionState> userActionStates) {
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
            InlineKeyboardMarkup inlineKeyboardMarkup = createInlineKeyboardMarkup("add_users", "delete_users", "edit_users");
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            message.setReplyMarkup(inlineKeyboardMarkup);
            return message;
        }
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(String addCallbackData, String deleteCallbackData, String editCallbackData) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton addButton = new InlineKeyboardButton("Add");
        addButton.setCallbackData(addCallbackData);

        InlineKeyboardButton deleteButton = new InlineKeyboardButton("Delete");
        deleteButton.setCallbackData(deleteCallbackData);

        InlineKeyboardButton editButton = new InlineKeyboardButton("Edit");
        editButton.setCallbackData(editCallbackData);

        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        keyboardButtonsRow.add(addButton);
        keyboardButtonsRow.add(deleteButton);
        keyboardButtonsRow.add(editButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow);

        inlineKeyboardMarkup.setKeyboard(rowList);
        return inlineKeyboardMarkup;
    }
}

