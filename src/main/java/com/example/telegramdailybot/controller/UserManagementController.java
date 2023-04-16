package com.example.telegramdailybot.controller;

import com.example.telegramdailybot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UserManagementController {

    private final UserService userService;

    @Autowired
    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    public String addUser(Update update) {
        return "User added successfully.";
    }

    public String deleteUser(Update update) {
        return "User deleted successfully.";
    }

    public String resetWinners(Update update) {
        userService.resetWinners(update.getMessage().getChatId());
        return "Winners reset successfully.";
    }

    public String showUsers(Update update) {
        List<String> fieldsToDisplay = Arrays.asList("name", "username", "haswon");
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("name", "имя");
        customHeaders.put("haswon", "выиграл");
        return userService.generateUserListMessage(update.getMessage().getChatId(), fieldsToDisplay, customHeaders);
    }

    public String findWinner(Update update) {
        return userService.findWinner(update.getMessage().getChatId());
    }

}

