package com.example.telegramdailybot;


import com.example.telegramdailybot.config.TelegramDailyBotProperties;
import com.example.telegramdailybot.controller.ChatManagementController;
import com.example.telegramdailybot.controller.NotificationManagementController;
import com.example.telegramdailybot.controller.UserManagementController;
import com.example.telegramdailybot.model.Chat;
import com.example.telegramdailybot.model.Notification;
import com.example.telegramdailybot.model.User;
import com.example.telegramdailybot.model.UserActionState;
import com.example.telegramdailybot.service.ChatGPT3Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
public class TelegramDailyBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TelegramDailyBot.class);
    private final ChatGPT3Service chatGpt3Service;
    private final TelegramDailyBotProperties properties;

    private final Map<Long, UserActionState> userActionStates = new HashMap<>();

    private final UserManagementController userManagementController;
    private final NotificationManagementController notificationManagementController;
    private final ChatManagementController chatManagementController;

    @Autowired
    public TelegramDailyBot(ChatGPT3Service chatGpt3Service,
                            TelegramDailyBotProperties properties,
                            UserManagementController userManagementController,
                            NotificationManagementController notificationManagementController,
                            ChatManagementController chatManagementController) {
        super(properties.getBotToken());
        this.chatGpt3Service = chatGpt3Service;
        this.properties = properties;
        this.userManagementController = userManagementController;
        this.notificationManagementController = notificationManagementController;
        this.chatManagementController = chatManagementController;
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
        if (update.getMessage().isCommand()) {
            handleCommand(update);
        } else {
            handleNonCommandTextMessage(update);
        }
    }

    private void handleCommand(Update update) {
        String command = update.getMessage().getText().split("@")[0]; // Remove the username from the command
        Long chatId = update.getMessage().getChatId();

        if (!chatManagementController.existsById(chatId) && !"/start".equalsIgnoreCase(command) && !"/getchatid".equalsIgnoreCase(command)) {
            sendChatMessage(chatId, "Вы не зарегистрированы в боте!");
            return;
        }

        switch (command.toLowerCase()) {
            case "/start" -> handleStartCommand(chatId);
            case "/getchatid" -> handleGetChatIdCommand(chatId);
            case "/next" -> sendChatMessage(userManagementController.nextWinner(update));
            case "/resetwinners" -> sendChatMessage(userManagementController.resetWinners(update));
            case "/showusers" -> sendChatMessage(userManagementController.showUsers(update));
            case "/shownotifications" -> sendChatMessage(notificationManagementController.showNotifications(update));
            case "/editusers" -> sendChatMessage(userManagementController.editUsersMessage(update, userActionStates));
            case "/editnotifications" ->
                    sendChatMessage(notificationManagementController.editNotificationsMessage(update, userActionStates));
            case "/editchats" -> sendChatMessage(chatManagementController.editChatsMessage(update));
            case "/askchatgpt3" -> askChatGPT3(update.getMessage(), chatId);
            default -> sendChatMessage(chatId, "Неизвестная команда!");
        }
    }


    private void handleNonCommandTextMessage(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        UserActionState userActionState = userActionStates.get(userId);

        if (userActionState == null) {
            logger.warn("UserActionState is null for user: {}", userId);
            // You can either return here or set a default value for userActionState
            sendChatMessage(chatId, "Сначала выберите команду из списка");
            return;
        }

        switch (userActionState) {
            case WAITING_FOR_USERS_TO_ADD ->
                    sendChatMessage(userManagementController.addUsers(update, userActionStates));
            case WAITING_FOR_USERS_TO_DELETE ->
                    sendChatMessage(userManagementController.deleteUsers(update, userActionStates));
            case WAITING_FOR_USERS_TO_EDIT ->
                    sendChatMessage(userManagementController.editUsers(update, userActionStates));
            case WAITING_FOR_NOTIFICATION_TO_ADD ->
                    sendChatMessage(notificationManagementController.addNotification(update, userActionStates));
            case WAITING_FOR_NOTIFICATION_TO_DELETE ->
                    sendChatMessage(notificationManagementController.deleteNotifications(update, userActionStates));
            case WAITING_FOR_NOTIFICATION_TO_EDIT ->
                    sendChatMessage(notificationManagementController.editNotification(update, userActionStates));
            case WAITING_FOR_CHATS_TO_ADD ->
                    sendChatMessage(chatManagementController.addChats(update, userActionStates));
            case WAITING_FOR_CHATS_TO_DELETE ->
                    sendChatMessage(chatManagementController.deleteChats(update, userActionStates));
            case WAITING_FOR_CHATS_TO_EDIT ->
                    sendChatMessage(chatManagementController.editChats(update, userActionStates));
            case WAITING_FOR_CHATGPT3_QUERY -> {
                sendChatMessage(chatId, "Подождите, пожалуйста, ChatGPT пишет ответ...");
                // Remove the user from the userAddingStates map
                userActionStates.remove(userId);
                chatGpt3Service.chat(text).thenAcceptAsync(responseText -> sendChatMessage(chatId, responseText));
            }
            case WAITING_FOR_CHAT_ID_TO_EDIT_USERS ->
                    sendChatMessage(userManagementController.editUsersByAdmin(update, userActionStates));
            case WAITING_FOR_CHAT_ID_TO_EDIT_NOTIFICATIONS ->
                    sendChatMessage(notificationManagementController.editNotificationsByAdmin(update, userActionStates));
            case WAITING_FOR_CHAT_ID_TO_ADD_USERS ->
                    sendChatMessage(userManagementController.addUsersByAdmin(update, userActionStates));
            case WAITING_FOR_CHAT_ID_TO_ADD_NOTIFICATION ->
                    sendChatMessage(notificationManagementController.addNotificationByAdmin(update, userActionStates));
        }
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String data = callbackQuery.getData();

        switch (data) {
            case "add_users" ->
                    sendChatMessage(userManagementController.initiateAddUsersProcess(update, userActionStates));
            case "delete_users" ->
                    sendChatMessage(userManagementController.initiateDeleteUsersProcess(update, userActionStates));
            case "edit_users" ->
                    sendChatMessage(userManagementController.initiateEditUsersProcess(update, userActionStates));
            case "add_notification" ->
                    sendChatMessage(notificationManagementController.initiateAddNotificationProcess(update, userActionStates));
            case "delete_notifications" ->
                    sendChatMessage(notificationManagementController.initiateDeleteNotificationsProcess(update, userActionStates));
            case "edit_notification" ->
                    sendChatMessage(notificationManagementController.initiateEditNotificationProcess(update, userActionStates));
            case "add_chats" ->
                    sendChatMessage(chatManagementController.initiateAddChatsProcess(update, userActionStates));
            case "delete_chats" ->
                    sendChatMessage(chatManagementController.initiateDeleteChatsProcess(update, userActionStates));
            case "edit_chats" ->
                    sendChatMessage(chatManagementController.initiateEditChatsProcess(update, userActionStates));
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

    private void askChatGPT3(Message message, Long chatId) {

        Long userId = message.getFrom().getId();

        userActionStates.put(userId, UserActionState.WAITING_FOR_CHATGPT3_QUERY);

        sendChatMessage(chatId, "Напишите свой вопрос ChatGPT3");
    }


    private void sendChatMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setDisableWebPagePreview(true);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to chat: {}", chatId, e);
        }
    }

    private void sendChatMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to chat: {}", message.getChatId(), e);
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
                LocalDateTime nowLocal = now.withZoneSameInstant(properties.getTimeZone()).toLocalDateTime().toLocalDate().atStartOfDay();
                // Calculate the number of days between the given day and the current time
                long daysBetween = ChronoUnit.DAYS.between(nowLocal, day);

                // If the days between the given day and now is a multiple of the frequency,
                // the notification is excluded
                if (daysBetween == 0) {
                    // Update the "day" value in the "skip_days" array
                    LocalDateTime newDay = day.plusDays(frequency);
                    ((ObjectNode) skipDay).put("day", newDay.format(dateFormatter));

                    // Update the notification's "datetimexcluded" field
                    notification.setDatetimexcluded(datetimexcluded);
                    notificationManagementController.save(notification);

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
        List<Notification> notifications = notificationManagementController.findAll();

        for (Notification notification : notifications) {
            ZonedDateTime notificationDateTime = notification.getDatetime();
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

            // Check if the current time is within the 1-minute time window of the notification's scheduled time
            if ((now.isEqual(notificationDateTime) || (now.isAfter(notificationDateTime) && now.isBefore(notificationDateTime.plusMinutes(1))))) {
                if (!isNotificationExcluded(notification, now)) {
                    Optional<Chat> optionalChat = chatManagementController.findById(notification.getChatid());
                    if (optionalChat.isPresent()) {
                        Chat chat = optionalChat.get();
                        String text = notification.getText();

                        // Replace "@name" and "@username" placeholders with the winner's name and username, if applicable
                        if (text.contains("@name") || text.contains("@username")) {
                            User winner = userManagementController.findWinner(chat.getTelegramchatid());
                            if (winner != null) {
                                text = text.replace("@name", winner.getName());
                                text = text.replace("@username", "@" + winner.getUsername());
                            } else {
                                text = "Участники для розыгрыша в этом чате отсутствуют";
                            }
                        }
                        sendChatMessage(chat.getTelegramchatid(), text);
                    } else {
                        notificationManagementController.delete(notification);
                    }
                }
                // Update the notification's scheduled time based on its repetition setting
                // This is outside the !isNotificationExcluded() check but still within the time range check
                switch (notification.getRepetition()) {
                    case "minutely" -> {
                        notification.setDatetime(notificationDateTime.plusMinutes(5));
                        notificationManagementController.save(notification);
                    }
                    case "hourly" -> {
                        notification.setDatetime(notificationDateTime.plusHours(1));
                        notificationManagementController.save(notification);
                    }
                    case "daily" -> {
                        notification.setDatetime(notificationDateTime.plusDays(1));
                        notificationManagementController.save(notification);
                    }
                    case "weekly" -> {
                        notification.setDatetime(notificationDateTime.plusWeeks(1));
                        notificationManagementController.save(notification);
                    }
                    case "monthly" -> {
                        notification.setDatetime(notificationDateTime.plusMonths(1));
                        notificationManagementController.save(notification);
                    }
                    case "once" -> notificationManagementController.delete(notification);
                }
            }
        }
    }


    @Override
    public String getBotUsername() {
        return properties.getBotUsername();
    }

}

