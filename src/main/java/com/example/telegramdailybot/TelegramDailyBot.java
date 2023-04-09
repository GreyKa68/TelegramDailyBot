package com.example.telegramdailybot;


import com.example.telegramdailybot.handler.*;
import com.example.telegramdailybot.model.Chat;
import com.example.telegramdailybot.model.Notification;
import com.example.telegramdailybot.model.User;
import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.repository.ChatRepository;
import com.example.telegramdailybot.repository.NotificationRepository;
import com.example.telegramdailybot.repository.UserRepository;
import com.example.telegramdailybot.service.ChatGPT3Service;
import com.example.telegramdailybot.util.NotificationUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Component
public class TelegramDailyBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramDailyBot.class);
    private final ChatGPT3Service chatGpt3Service;
    private final ChatDeletionHandler chatDeletionHandler;
    private final ChatEditHandler chatEditHandler;
    private final UserDeletionHandler userDeletionHandler;
    private final UserEditHandler userEditHandler;
    private final NotificationDeletionHandler notificationDeletionHandler;
    private final NotificationEditHandler notificationEditHandler;
    private final TelegramDailyBotProperties properties;
    private final ChatRepository chatRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final Map<Long, UserActionState> userActionStates = new HashMap<>();

    @Autowired
    public TelegramDailyBot(ChatGPT3Service chatGpt3Service, ChatEditHandler chatEditHandler, ChatDeletionHandler chatDeletionHandler, NotificationEditHandler notificationEditHandler, NotificationDeletionHandler notificationDeletionHandler, UserEditHandler userEditHandler, UserDeletionHandler userDeletionHandler,
                            TelegramDailyBotProperties properties,
                            ChatRepository chatRepository,
                            NotificationRepository notificationRepository,
                            UserRepository userRepository) {
        super(properties.getBotToken());
        this.chatGpt3Service = chatGpt3Service;
        this.chatEditHandler = chatEditHandler;
        this.chatDeletionHandler = chatDeletionHandler;
        this.notificationEditHandler = notificationEditHandler;
        this.notificationDeletionHandler = notificationDeletionHandler;
        this.userEditHandler = userEditHandler;
        this.userDeletionHandler = userDeletionHandler;
        this.properties = properties;
        this.chatRepository = chatRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessageWithText(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleMessageWithText(Update update) {
        Message message = update.getMessage();
        String text = message.getText();
        Long chatId = message.getChatId();

        if (isCommand(text)) {
            handleCommand(message, text, chatId);
        } else {
            handleNonCommandTextMessage(message, text, chatId);
        }
    }

    private boolean isCommand(String text) {
        return text.startsWith("/");
    }

    private void handleCommand(Message message, String text, Long chatId) {
        String command = text.split("@")[0]; // Remove the username from the command

        if (!chatRepository.existsById(chatId) && !"/start".equalsIgnoreCase(command) && !"/getchatid".equalsIgnoreCase(command)) {
            sendChatMessage(chatId, "Вы не зарегистрированы в боте!");
            return;
        }

        switch (command.toLowerCase()) {
            case "/start" -> handleStartCommand(chatId);
            case "/getchatid" -> handleGetChatIdCommand(chatId);
            default -> handleChatCommand(message, command, chatId);
        }
    }

    private void handleNonCommandTextMessage(Message message, String text, Long chatId) {
        Long userId = message.getFrom().getId();
        UserActionState userActionState = userActionStates.get(userId);

        if (userActionState == null) {
            logger.warn("UserActionState is null for user: {}", userId);
            // You can either return here or set a default value for userActionState
            sendChatMessage(chatId, "Сначала выберите команду");
            return;
        }

        switch (userActionState) {
            case WAITING_FOR_USERS_TO_ADD -> handleUserAdding(message, text, chatId, userId);
            case WAITING_FOR_USERS_TO_DELETE -> {
                SendMessage msg = userDeletionHandler.handleUserDeleting(userActionStates, message, text, chatId, userId);
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message to user: {}", userId, e);
                }
            }
            case WAITING_FOR_USERS_TO_EDIT -> {
                SendMessage msg = userEditHandler.handleUserEditing(userActionStates, message, text, chatId, userId);
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message to user: {}", userId, e);
                }
            }
            case WAITING_FOR_NOTIFICATION_TO_ADD -> handleNotificationAdding(message, text, chatId, userId);
            case WAITING_FOR_NOTIFICATION_TO_DELETE -> {
                SendMessage msg = notificationDeletionHandler.handleNotificationDeleting(userActionStates, message, text, chatId, userId);
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message to user: {}", userId, e);
                }
            }
            case WAITING_FOR_NOTIFICATION_TO_EDIT -> {
                SendMessage msg = notificationEditHandler.handleNotificationEditing(userActionStates, message, text, chatId, userId);
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message to user: {}", userId, e);
                }
            }
            case WAITING_FOR_CHATS_TO_ADD -> handleChatsAdding(message, text, chatId, userId);
            case WAITING_FOR_CHATS_TO_DELETE -> {
                SendMessage msg = chatDeletionHandler.handleChatDeleting(userActionStates, message, text, chatId, userId);
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message to user: {}", userId, e);
                }
            }
            case WAITING_FOR_CHATS_TO_EDIT -> {
                SendMessage msg = chatEditHandler.handleChatEditing(userActionStates, message, text, chatId, userId);
                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    logger.error("Error sending message to user: {}", userId, e);
                }
            }
            case WAITING_FOR_CHATGPT3_QUERY -> {
                sendChatMessage(chatId, "Подождите, пожалуйста, ChatGPT пишет ответ...");
                // Remove the user from the userAddingStates map
                userActionStates.remove(userId);
                chatGpt3Service.chat(text).thenAcceptAsync(responseText -> {
                    SendMessage responseMsg = new SendMessage();
                    responseMsg.setChatId(chatId.toString());
                    responseMsg.setText(responseText);

                    try {
                        execute(responseMsg);
                    } catch (TelegramApiException e) {
                        logger.error("Error sending message to user: {}", userId, e);
                    }

                });
            }
        }
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();
        Long userId = callbackQuery.getFrom().getId();
        Long chatId = callbackQuery.getMessage().getChatId();

        switch (data) {
            case "add_users" -> initiateAddUsersProcess(userId, chatId);
            case "delete_users" -> initiateDeleteUsersProcess(userId, chatId);
            case "edit_user" -> initiateEditUsersProcess(userId, chatId);
            case "add_notification" -> initiateAddNotificationProcess(userId, chatId);
            case "delete_notifications" -> initiateDeleteNotificationsProcess(userId, chatId);
            case "edit_notification" -> initiateEditNotificationProcess(userId, chatId);
            case "add_chats" -> initiateAddChatsProcess(userId, chatId);
            case "delete_chats" -> initiateDeleteChatsProcess(userId, chatId);
            case "edit_chats" -> initiateEditChatsProcess(userId, chatId);
        }

        // Acknowledge the callback query
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQuery.getId());
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error("Error answering callback query", e);
        }
    }

    private void handleStartCommand(Long chatId) {
        String welcomeMessage = """
                🎉 Добро пожаловать в DailyBot2.0! 🤖

                🌟 Ваш личный помощник для организации ежедневных задач и оповещений в Телеграм-чате! 📅

                🚀 Что мы можем сделать вместе:
                1️⃣ Лотерея пользователей: выбирайте победителей и добавляйте новых участников 🏆
                2️⃣ Персонализированные уведомления: создавайте и редактируйте напоминания 🔔
                3️⃣ Умные ответы с ChatGPT: задавайте вопросы и получайте развернутые ответы 🧠💬

                🤩 Приятного использования! Вместе мы сделаем ваш чат продуктивнее и веселее! 🎯""";

        sendChatMessage(chatId, welcomeMessage);
    }

    private void handleGetChatIdCommand(Long chatId) {
        sendChatMessage(chatId, "ID вашего чата: " + chatId);
    }

    private void handleChatCommand(Message message, String command, Long chatId) {
        switch (command.toLowerCase()) {
            case "/next" -> nextWinner(chatId);
            case "/resetwinners" -> resetWinners(chatId);
            case "/showusers" -> showUsers(chatId);
            case "/shownotifications" -> showNotifications(chatId);
            case "/editusers" -> editUsers(chatId);
            case "/editnotifications" -> editNotifications(chatId);
            case "/editchats" -> editChats(message, chatId);
            case "/askchatgpt3" -> askChatGPT3(message, chatId);
            default -> sendChatMessage(chatId, "Неизвестная команда!");
        }
    }

    private void askChatGPT3(Message message, Long chatId) {

        Long userId = message.getFrom().getId();

        userActionStates.put(userId, UserActionState.WAITING_FOR_CHATGPT3_QUERY);

        SendMessage responseMessage = new SendMessage();
        responseMessage.setChatId(chatId.toString());
        responseMessage.setText("Напишите свой вопрос ChatGPT3");
        try {
            execute(responseMessage);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateAddUsersProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_USERS_TO_ADD);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        String text = """
                Пожалуйста, вышлите через запятую: имя, @username. Например:

                Вася,@vasyatelegram
                Петя,@evilusername
                Эвелина,@evacool""";
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateAddChatsProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_CHATS_TO_ADD);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        String text = """
                Пожалуйста, вышлите через запятую: ID, название чата, роль. Например:

                12345678, Чат команды1, admin
                12345678, Чат команды2, user
                12345678, Иван Иванов, admin""";
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateAddNotificationProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_NOTIFICATION_TO_ADD);

        String text = """
                Пожалуйста, пришлите уведомление согласно следующему шаблону. Для удобства шаблон можно скопировать, вставить и отредактировать

                Текст уведомления: Все на дейли, сегодня шарит @name, @username!
                Дата и время: 2023-04-06T14:00
                Частота: {once|minutely|hourly|daily|weekly|monthly|yearly}
                Исключения:
                  - Исключить СБ и ВС
                  - Исключить дни:
                    * 2023-04-12 (every 7 days)
                    * 2023-04-24 (every 21 days)
                    * 2023-04-07 (every 7 days)""";

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateDeleteUsersProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_USERS_TO_DELETE);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        String text = """
                Пожалуйста, вышлите ID участников, которых вы хотите удалить, каждый ID с новой строчки. Например:

                10
                11
                12""";
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateDeleteChatsProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_CHATS_TO_DELETE);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        String text = """
                Пожалуйста, вышлите ID чатов, которых вы хотите удалить, каждый ID с новой строчки. Например:

                10
                11
                12""";
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateDeleteNotificationsProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_NOTIFICATION_TO_DELETE);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        String text = """
                Пожалуйста, вышлите ID уведомлений, которые вы хотите удалить, каждый ID с новой строчки. Например:

                10
                11
                12""";
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateEditUsersProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_USERS_TO_EDIT);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        String text = """
                Пожалуйста, вышлите через запятую: ID участника, которого вы хотите изменить, имя, username. Например:

                10,Вася,vasyatelegram
                11,Петя,evilusername
                12,Эвелина,evacool""";
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateEditChatsProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_CHATS_TO_EDIT);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        String text = """
                Пожалуйста, вышлите через запятую: ID чата, который вы хотите изменить, название, роль. Например:

                10,Scrum команда1,
                11,Петя,admin
                12,Scrum команда2,""";
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateEditNotificationProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_NOTIFICATION_TO_EDIT);

        String text = """
                Пожалуйста, пришлите измененное уведомление согласно следующему шаблону. Для удобства скопируйте предыдущую версию уведомления и измените ее
                                
                ID 11
                Текст уведомления: Все на дейли, сегодня шарит @name, @username!
                Дата и время: 2023-04-06T14:00
                Частота: {once|minutely|hourly|daily|weekly|monthly|yearly}
                Исключения:
                  - Исключить СБ и ВС
                  - Исключить дни:
                    * 2023-04-12 (every 7 days)
                    * 2023-04-24 (every 21 days)
                    * 2023-04-07 (every 7 days)""";

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    @Transactional
    private void handleUserAdding(Message message, String text, Long chatId, Long userId) {
        // Parse and add users from the message text
        String[] lines = text.split("\\n");

        for (String line : lines) {
            String[] parts = line.split(",", 2);
            if (parts.length == 2) {
                String name = parts[0].trim();
                String username = parts[1].trim().replace("@", "");

                User user = new User();
                user.setName(name);
                user.setUsername(username);
                user.setChatid(chatId);
                user.setHaswon(false);

                userRepository.save(user);
            }
        }
        // Remove the user from the userAddingStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Участники успешно добавлены");
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    @Transactional
    private void handleChatsAdding(Message message, String text, Long chatId, Long userId) {
        // Parse and add chats from the message text
        String[] lines = text.split("\\n");

        for (String line : lines) {
            String[] parts = line.split(",", 3);
            if (parts.length == 3) {
                Long telegramchatid = Long.parseLong(parts[0].trim());
                String name = parts[1].trim();
                String role = parts[2].trim();

                Chat chat = new Chat();
                chat.setTelegramchatid(telegramchatid);
                chat.setName(name);
                chat.setRole(role);

                chatRepository.save(chat);
            }
        }
        // Remove the user from the userAddingStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Чаты успешно добавлены");
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    @Transactional
    private void handleNotificationAdding(Message message, String text, Long chatId, Long userId) {
        // Parse the notification from the message text
        Notification notification = NotificationUtils.parseNotificationText(text);
        if (notification == null) {
            // Send an error message if the text could not be parsed
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText("Ошибка при добавлении уведомления. Пожалуйста, проверьте соответствие шаблону");
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                logger.error("Error sending message to user: {}", userId, e);
            }
            return;
        }
        // Set the chat ID
        notification.setChatid(chatId);

        // Save the notification
        notificationRepository.save(notification);

        // Remove the user from the userAddingStates map
        userActionStates.remove(userId);

        // Send a confirmation message to the user
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId.toString());
        msg.setText("Уведомление успешно добавлено");
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    // This method allows the user to edit Users in the specified chat.
    // It displays a list of Users and provides an inline keyboard with Add, Delete, and Edit buttons.
    private void editUsers(Long chatId) {
        List<User> users = userRepository.findByChatid(chatId);
        if (users.isEmpty()) {
            sendChatMessage(chatId, "Участники розыгрышей в этом чате отсутствуют");
            return;
        }

        StringBuilder sb = new StringBuilder("Участники розыгрышей в этом чате:\n\n");
        for (User user : users) {
            sb.append("ID: ").append(user.getId()).append(", ")
                    .append(user.getName()).append(", @").append(user.getUsername()).append('\n');
        }

        // Create an inline keyboard markup for editing Users.
        InlineKeyboardMarkup inlineKeyboardMarkup = createInlineKeyboardMarkup("add_users", "delete_users", "edit_user");
        // Send the message with the inline keyboard to the chat.
        sendMessageWithInlineKeyboard(chatId, sb.toString(), inlineKeyboardMarkup);
    }

    // This method allows the user to edit Notifications in the specified chat.
    // It displays a list of Notifications and provides an inline keyboard with Add, Delete, and Edit buttons.
    private void editNotifications(Long chatId) {
        List<Notification> notifications = notificationRepository.findByChatid(chatId);
        if (notifications.isEmpty()) {
            sendChatMessage(chatId, "Уведомления для этого чата отсутствуют");
            return;
        }

        showNotifications(chatId);
        // Create an inline keyboard markup for editing Notifications.
        InlineKeyboardMarkup inlineKeyboardMarkup = createInlineKeyboardMarkup("add_notification", "delete_notifications", "edit_notification");
        // Send the message with the inline keyboard to the chat.
        sendMessageWithInlineKeyboard(chatId, "Выберите действие:", inlineKeyboardMarkup);
    }

    // This method allows the user to edit Chats.
    // It displays a list of Chats and provides an inline keyboard with Add, Delete, and Edit buttons.
    // The user must be an admin and in a private chat to edit Chats.
    private void editChats(Message message, Long chatId) {
        // Check if the chat is private
        if (message.getChat().isUserChat()) {
            Optional<Chat> optionalChat = chatRepository.findById(chatId);
            if (optionalChat.isPresent()) {
                Chat chatTemp = optionalChat.get();

                // Check if the user has 'admin' role
                if ("admin".equalsIgnoreCase(chatTemp.getRole())) {
                    List<Chat> chatList = chatRepository.findAll();
                    StringBuilder sb = new StringBuilder("Список чатов:\n\n");
                    for (Chat chat : chatList) {
                        sb.append(chat.getTelegramchatid()).append(", ")
                                .append(chat.getName()).append(", ").append(chat.getRole()).append('\n');
                    }

                    // Create an inline keyboard markup for editing Chats.
                    InlineKeyboardMarkup inlineKeyboardMarkup = createInlineKeyboardMarkup("add_chats", "delete_chats", "edit_chats");
                    // Send the message with the inline keyboard to the chat.
                    sendMessageWithInlineKeyboard(chatId, sb.toString(), inlineKeyboardMarkup);
                } else {
                    sendChatMessage(chatId, "У вас нет прав администратора для редактирования чата!");
                }
            } else {
                sendChatMessage(chatId, "Чат не зарегистрирован!");
            }
        } else {
            sendChatMessage(chatId, "Команда /editchats доступна только в приватных чатах!");
        }
    }

    // This method creates an inline keyboard markup with Add, Delete, and Edit buttons.
    // The provided callback data is used to set the appropriate callback for each button.
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

    // This method sends a message with the provided text and inline keyboard markup to the specified chat.
    private void sendMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup inlineKeyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to chat: {}", chatId, e);
        }
    }

    private void resetWinners(Long chatId) {
        for (User user : userRepository.findAll()) {
            user.setHaswon(false);
            userRepository.save(user);
        }
        sendChatMessage(chatId, "Победители сброшены!");
    }

    private void showUsers(Long chatId) {
        List<User> users = userRepository.findByChatid(chatId);
        if (users.isEmpty()) {
            sendChatMessage(chatId, "Участники розыгрышей в этом чате отсутствуют");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Участники розыгрышей в этом чате:\n");
        for (User user : users) {
            sb.append(user.getName()).append(", @").append(user.getUsername()).append(", выиграл: ").append(user.isHaswon()).append("\n");
        }
        sendChatMessage(chatId, sb.toString());
    }

    private void showNotifications(Long chatId) {
        List<Notification> notifications = notificationRepository.findByChatid(chatId);
        if (notifications.isEmpty()) {
            sendChatMessage(chatId, "Уведомления для этого чата отсутствуют");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Уведомления для этого чата: \n\n");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmZ");

        // Specify the +3 GMT timezone
        ZoneId timeZone = ZoneId.of("GMT+3");

        for (Notification notification : notifications) {
            sb.append("ID: ").append(notification.getId()).append("\n");
            sb.append("Текст уведомления: ").append(notification.getText()).append("\n");
            sb.append("Дата и время: ").append(notification.getDatetime().withZoneSameInstant(ZoneId.of("GMT+3")).format(dateTimeFormatter)).append("\n");
            sb.append("Частота: ").append(notification.getRepetition()).append("\n");

            if (notification.getDatetimexcluded() != null) {
                sb.append("Исключения:\n");

                if (notification.getDatetimexcluded().get("weekends").asBoolean()) {
                    sb.append("  - Исключить СБ и ВС\n");
                }

                ArrayNode skipDays = (ArrayNode) notification.getDatetimexcluded().get("skip_days");
                if (skipDays != null) {
                    sb.append("  - Исключить дни:\n");
                    for (JsonNode skipDay : skipDays) {
                        int frequency = skipDay.get("frequency").asInt();
                        String dayStr = skipDay.get("day").asText();
                        sb.append("    * ").append(dayStr).append(" (every ").append(frequency).append(" days)\n");
                    }
                }
            }

            sb.append("\n");
        }

        sendChatMessage(chatId, sb.toString());
    }

    private User findWinner(Long chatId) {
        List<User> users = userRepository.findByChatid(chatId);

        if (users.isEmpty()) {
            return null;
        }

        List<User> usersWithoutWin = new ArrayList<>();
        for (User user : users) {
            if (!user.isHaswon()) {
                usersWithoutWin.add(user);
            }
        }

        if (usersWithoutWin.isEmpty()) {
            for (User user : users) {
                user.setHaswon(false);
                userRepository.save(user);
            }
            usersWithoutWin.addAll(users);
        }

        Random random = new Random();
        int winnerIndex = random.nextInt(usersWithoutWin.size());
        User winner = usersWithoutWin.get(winnerIndex);
        winner.setHaswon(true);
        userRepository.save(winner);

        return winner;
    }

    private void nextWinner(Long chatId) {
        User winner = findWinner(chatId);

        if (winner == null) {
            sendChatMessage(chatId, "Участники для розыгрыша в этом чате отсутствуют");
        } else {
            sendChatMessage(chatId, "Участник " + winner.getName() + ", @" + winner.getUsername() + " выиграл!");
        }
    }

    private void sendChatMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to chat: {}", chatId, e);
        }
    }

    // This method checks if a given notification is excluded from being sent at the specified time.
    private boolean isNotificationExcluded(Notification notification, ZonedDateTime now) {
        // Retrieve the "datetimexcluded" configuration from the notification
        JsonNode datetimexcluded = notification.getDatetimexcluded();
        // If "datetimexcluded" is not set, the notification is not excluded.
        if (datetimexcluded == null) {
            return false;
        }

        // Check if weekends are excluded from the notification schedule
        boolean isWeekendExcluded = datetimexcluded.get("weekends").asBoolean();
        if (isWeekendExcluded) {
            // If the current day is Saturday (6) or Sunday (7), the notification is excluded
            int dayOfWeek = now.getDayOfWeek().getValue();
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                return true;
            }
        }

        // Check if specific days are excluded from the notification schedule
        ArrayNode skipDays = (ArrayNode) datetimexcluded.get("skip_days");
        if (skipDays != null) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // Iterate through each day specified in the "skip_days" array
            for (JsonNode skipDay : skipDays) {
                // Retrieve the frequency and day for the current "skip_day" entry
                int frequency = skipDay.get("frequency").asInt();
                String dayStr = skipDay.get("day").asText();
                LocalDateTime day = LocalDate.parse(dayStr, dateFormatter).atStartOfDay();
                // Calculate the number of days between the given day and the current time
                long daysBetween = java.time.Duration.between(day, now).toDays();

                // If the days between the given day and now is a multiple of the frequency,
                // the notification is excluded
                if (daysBetween % frequency == 0) {
                    // Update the "day" value in the "skip_days" array
                    LocalDateTime newDay = day.plusDays(frequency);
                    ((ObjectNode) skipDay).put("day", newDay.format(dateFormatter));

                    // Update the notification's "datetimexcluded" field
                    notification.setDatetimexcluded(datetimexcluded);
                    notificationRepository.save(notification);

                    return true;
                }
            }
        }

        // If none of the exclusion conditions apply, the notification is not excluded
        return false;
    }


    //This method is responsible for checking and sending notifications to chats.
    //It is scheduled to run every 60 seconds after an initial delay of 1 second.
    @Scheduled(fixedRate = 60000, initialDelay = 1000) // Run every 60 seconds
    public void checkAndSendNotifications() {
        List<Notification> notifications = notificationRepository.findAll();

        for (Notification notification : notifications) {
            ZonedDateTime notificationDateTime = notification.getDatetime();
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

            // Check if the current time is within the 1-minute time window of the notification's scheduled time
            if ((now.isEqual(notificationDateTime) || (now.isAfter(notificationDateTime) && now.isBefore(notificationDateTime.plusMinutes(1))))) {
                if (!isNotificationExcluded(notification, now)) {
                    Optional<Chat> optionalChat = chatRepository.findById(notification.getChatid());
                    if (optionalChat.isPresent()) {
                        Chat chat = optionalChat.get();
                        String text = notification.getText();

                        // Replace "@name" and "@username" placeholders with the winner's name and username, if applicable
                        if (text.contains("@name") || text.contains("@username")) {
                            User winner = findWinner(chat.getTelegramchatid());
                            if (winner != null) {
                                text = text.replace("@name", winner.getName());
                                text = text.replace("@username", "@" + winner.getUsername());
                            } else {
                                text = "Участники для розыгрыша в этом чате отсутствуют";
                            }
                        }
                        sendChatMessage(chat.getTelegramchatid(), text);
                    } else {
                        notificationRepository.delete(notification);
                    }
                }
                // Update the notification's scheduled time based on its repetition setting
                // This is outside the !isNotificationExcluded() check but still within the time range check
                switch (notification.getRepetition()) {
                    case "minutely" -> {
                        notification.setDatetime(notificationDateTime.plusMinutes(5));
                        notificationRepository.save(notification);
                    }
                    case "hourly" -> {
                        notification.setDatetime(notificationDateTime.plusHours(1));
                        notificationRepository.save(notification);
                    }
                    case "daily" -> {
                        notification.setDatetime(notificationDateTime.plusDays(1));
                        notificationRepository.save(notification);
                    }
                    case "weekly" -> {
                        notification.setDatetime(notificationDateTime.plusWeeks(1));
                        notificationRepository.save(notification);
                    }
                    case "monthly" -> {
                        notification.setDatetime(notificationDateTime.plusMonths(1));
                        notificationRepository.save(notification);
                    }
                    case "once" -> notificationRepository.delete(notification);
                }
            }
        }
    }


    @Override
    public String getBotUsername() {
        return properties.getBotUsername();
    }

}
