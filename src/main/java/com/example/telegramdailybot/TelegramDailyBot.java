package com.example.telegramdailybot;


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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Component
public class TelegramDailyBot extends TelegramLongPollingBot {

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

    private Map<Long, UserActionState> userActionStates = new HashMap<>();


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

    private static final Logger logger = LoggerFactory.getLogger(TelegramDailyBot.class);

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
            case "/start":
                handleStartCommand(chatId);
                break;
            case "/getchatid":
                handleGetChatIdCommand(chatId);
                break;
            default:
                handleChatCommand(message, command, chatId);
                break;
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
        sendChatMessage(chatId, "Ваш бот DailyBot2.0 готов к использованию! \uD83C\uDF89 Бот поможет вам в организации ежедневных задач и оповещений для группы. Вы можете добавлять пользователей, назначать победителей, управлять уведомлениями и многое другое. Чтобы узнать доступные команды, используйте /help. Приятного использования! \uD83D\uDE0A");
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
        message.setText("Пожалуйста, вышлите через запятую: имя, username. Например:\n\nВася,vasyatelegram\nПетя,evilusername\nЭвелина,evacool");
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
        message.setText("Please send chat information in the following format:\n\nID, Name, Role\n\nYou can add multiple chats, one per line.");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateAddNotificationProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_NOTIFICATION_TO_ADD);

        StringBuilder sb = new StringBuilder();
        sb.append("Пожалуйста, пришлите уведомление согласно следующему шаблону. Для удобства шаблон можно скопировать, вставить и отредактировать\n\n");
        sb.append("Текст уведомления: Все на дейли, сегодня шарит @name, @username!\n");
        sb.append("Дата и время: 2023-04-06T14:00\n");
        sb.append("Частота: minutely\n");
        sb.append("Исключения:\n");
        sb.append("  - Исключить СБ и ВС\n");
        sb.append("  - Исключить дни:\n");
        sb.append("    * 2023-04-12 (every 7 days)\n");
        sb.append("    * 2023-04-24 (every 21 days)\n");
        sb.append("    * 2023-04-07 (every 7 days)");
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(sb.toString());
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
        message.setText("Пожалуйста, вышлите ID участников, которых вы хотите удалить, каждый ID с новой строчки. Например:\n\n10\n11\n12");
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
        message.setText("Please send chat IDs, one per line, that you want to delete. For example:\n\n10\n11\n12");
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
        message.setText("Пожалуйста, вышлите ID уведомлений, которые вы хотите удалить, каждый ID с новой строчки. Например:\n\n10\n11\n12");
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
        message.setText("Пожалуйста, вышлите через запятую: ID участника, которого вы хотите изменить, имя, username. Например:\n\n10,Вася,vasyatelegram\n11,Петя,evilusername\n12,Эвелина,evacool");
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
        message.setText("Пожалуйста, вышлите через запятую: ID чата, который вы хотите изменить, название, роль. Например:\n\n10,Scrum команда1,\n11,Петя,admin\n12,Scrum команда2,");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void initiateEditNotificationProcess(Long userId, Long chatId) {
        userActionStates.put(userId, UserActionState.WAITING_FOR_NOTIFICATION_TO_EDIT);

        StringBuilder sb = new StringBuilder();
        sb.append("Пожалуйста, пришлите измененное уведомление согласно следующему шаблону. Для удобства скопируйте предыдущую версию уведомления и измените ее\n\n");
        sb.append("ID 11\n");
        sb.append("Текст уведомления: Все на дейли, сегодня шарит @name, @username!\n");
        sb.append("Дата и время: 2023-04-06T14:00\n");
        sb.append("Частота: minutely\n");
        sb.append("Исключения:\n");
        sb.append("  - Исключить СБ и ВС\n");
        sb.append("  - Исключить дни:\n");
        sb.append("    * 2023-04-12 (every 7 days)\n");
        sb.append("    * 2023-04-24 (every 21 days)\n");
        sb.append("    * 2023-04-07 (every 7 days)");
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(sb.toString());
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
        msg.setText("Users have been added successfully.");
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
        msg.setText("Chats have been added successfully.");
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
            msg.setText("Error parsing the notification text. Please check the format and try again.");
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
        msg.setText("Notifications have been added successfully.");
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to user: {}", userId, e);
        }
    }

    private void editUsers(Long chatId) {
        List<User> users = userRepository.findByChatid(chatId);
        if (users.isEmpty()) {
            sendChatMessage(chatId, "No users found!");
            return;
        }

        StringBuilder sb = new StringBuilder("List of users:\n\n");
        for (User user : users) {
            sb.append("ID: " + user.getId()).append(", ")
                    .append(user.getName()).append(", @").append(user.getUsername()).append('\n');
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton addButton = new InlineKeyboardButton("Add");
        addButton.setCallbackData("add_users");

        InlineKeyboardButton deleteButton = new InlineKeyboardButton("Delete");
        deleteButton.setCallbackData("delete_users");

        InlineKeyboardButton editButton = new InlineKeyboardButton("Edit");
        editButton.setCallbackData("edit_user");

        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        keyboardButtonsRow.add(addButton);
        keyboardButtonsRow.add(deleteButton);
        keyboardButtonsRow.add(editButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow);

        inlineKeyboardMarkup.setKeyboard(rowList);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(sb.toString());
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to chat: {}", chatId, e);
        }
    }

    private void editNotifications(Long chatId) {
        List<Notification> notifications = notificationRepository.findByChatid(chatId);
        if (notifications.isEmpty()) {
            sendChatMessage(chatId, "No notifications found!");
            return;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton addButton = new InlineKeyboardButton("Add");
        addButton.setCallbackData("add_notification");

        InlineKeyboardButton deleteButton = new InlineKeyboardButton("Delete");
        deleteButton.setCallbackData("delete_notifications");

        InlineKeyboardButton editButton = new InlineKeyboardButton("Edit");
        editButton.setCallbackData("edit_notification");

        List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
        keyboardButtonsRow.add(addButton);
        keyboardButtonsRow.add(deleteButton);
        keyboardButtonsRow.add(editButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow);

        showNotifications(chatId);

        inlineKeyboardMarkup.setKeyboard(rowList);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Выберите действие:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to chat: {}", chatId, e);
        }
    }

    private void editChats(Message message, Long chatId) {
        // Check if the chat is private
        if (message.getChat().isUserChat()) {
            Optional<Chat> optionalChat = chatRepository.findById(chatId);
            if (optionalChat.isPresent()) {
                Chat chatTemp = optionalChat.get();

                // Check if the user has 'admin' role
                if ("admin".equalsIgnoreCase(chatTemp.getRole())) {
                    List<Chat> chatList = chatRepository.findAll();
                    StringBuilder sb = new StringBuilder("List of chats:\n\n");
                    for (Chat chat : chatList) {
                        sb.append(chat.getTelegramchatid()).append(", ")
                                .append(chat.getName()).append(", ").append(chat.getRole()).append('\n');
                    }

                    InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

                    InlineKeyboardButton addButton = new InlineKeyboardButton("Add");
                    addButton.setCallbackData("add_chats");

                    InlineKeyboardButton deleteButton = new InlineKeyboardButton("Delete");
                    deleteButton.setCallbackData("delete_chats");

                    InlineKeyboardButton editButton = new InlineKeyboardButton("Edit");
                    editButton.setCallbackData("edit_chats");

                    List<InlineKeyboardButton> keyboardButtonsRow = new ArrayList<>();
                    keyboardButtonsRow.add(addButton);
                    keyboardButtonsRow.add(deleteButton);
                    keyboardButtonsRow.add(editButton);

                    List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
                    rowList.add(keyboardButtonsRow);


                    inlineKeyboardMarkup.setKeyboard(rowList);
                    SendMessage msg = new SendMessage();
                    msg.setChatId(chatId.toString());
                    msg.setText(sb.toString());
                    msg.setReplyMarkup(inlineKeyboardMarkup);

                    try {
                        execute(msg);
                    } catch (TelegramApiException e) {
                        logger.error("Error sending message to chat: {}", chatId, e);
                    }
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

    private void resetWinners(Long chatId) {
        for (User user : userRepository.findAll()) {
            user.setHaswon(false);
            userRepository.save(user);
        }
        sendChatMessage(chatId, "Выигравшие сброшены!");
    }

    private void showUsers(Long chatId) {
        List<User> users = userRepository.findByChatid(chatId);
        StringBuilder sb = new StringBuilder();
        sb.append("Участники розыгрышей в этом чате:\n");
        for (User user : users) {
            sb.append(user.getName() + ", @" + user.getUsername() + ", выиграл: " + user.isHaswon() + "\n");
        }
        sendChatMessage(chatId, sb.toString());
    }

    private void showNotifications(Long chatId) {
        List<Notification> notifications = notificationRepository.findByChatid(chatId);
        if (notifications.isEmpty()) {
            sendChatMessage(chatId, "Уведомлений для этого чата нет!");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Уведомления для этого чата: \n\n");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

        for (Notification notification : notifications) {
            sb.append("ID: ").append(notification.getId()).append("\n");
            sb.append("Текст уведомления: ").append(notification.getText()).append("\n");
            sb.append("Дата и время: ").append(notification.getDatetime().format(dateTimeFormatter)).append("\n");
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
            sendChatMessage(chatId, "Не найдено ни одного участника в розыгрыше!");
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

    private boolean isNotificationExcluded(Notification notification, LocalDateTime now) {
        JsonNode datetimexcluded = notification.getDatetimexcluded();
        if (datetimexcluded == null) {
            return false;
        }

        boolean isWeekendExcluded = datetimexcluded.get("weekends").asBoolean();
        if (isWeekendExcluded) {
            int dayOfWeek = now.getDayOfWeek().getValue();
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                return true;
            }
        }

        ArrayNode skipDays = (ArrayNode) datetimexcluded.get("skip_days");
        if (skipDays != null) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            ObjectMapper objectMapper = new ObjectMapper();
            for (JsonNode skipDay : skipDays) {
                int frequency = skipDay.get("frequency").asInt();
                String dayStr = skipDay.get("day").asText();
                LocalDateTime day = LocalDate.parse(dayStr, dateFormatter).atStartOfDay();
                long daysBetween = java.time.Duration.between(day, now).toDays();

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

        return false;
    }

    @Scheduled(fixedRate = 60000, initialDelay = 1000) // Run every 60 seconds
    public void checkAndSendNotifications() {
        List<Notification> notifications = notificationRepository.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        for (Notification notification : notifications) {
            LocalDateTime notificationDateTime = notification.getDatetime();
            LocalDateTime now = LocalDateTime.now();
            //logger.info("Notification: {}, now: {}", notificationDateTime, now);

            if ((now.isEqual(notificationDateTime) || (now.isAfter(notificationDateTime) && now.isBefore(notificationDateTime.plusMinutes(1))))) {
                if (!isNotificationExcluded(notification, now)) {
                    Optional<Chat> optionalChat = chatRepository.findById(notification.getChatid());
                    if (optionalChat.isPresent()) {
                        Chat chat = optionalChat.get();
                        String text = notification.getText();

                        if (text.contains("@name") || text.contains("@username")) {
                            User winner = findWinner(chat.getTelegramchatid());
                            if (winner != null) {
                                text = text.replace("@name", winner.getName());
                                text = text.replace("@username", "@" + winner.getUsername());
                            } else {
                                text = "There are no users for this chat!";
                            }
                        }
                        sendChatMessage(chat.getTelegramchatid(), text);
                    } else {
                        notificationRepository.delete(notification);
                    }
                }
                // Move the switch statement here, outside the !isNotificationExcluded() check but still within the time range check
                switch (notification.getRepetition()) {
                    case "minutely" -> {
                        notification.setDatetime(notificationDateTime.plusMinutes(2));
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
