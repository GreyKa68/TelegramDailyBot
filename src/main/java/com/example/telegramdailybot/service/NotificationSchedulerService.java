package com.example.telegramdailybot.service;

import com.example.telegramdailybot.TelegramDailyBot;
import com.example.telegramdailybot.config.TelegramDailyBotProperties;
import com.example.telegramdailybot.controller.ChatManagementController;
import com.example.telegramdailybot.controller.NotificationManagementController;
import com.example.telegramdailybot.controller.UserManagementController;
import com.example.telegramdailybot.model.Chat;
import com.example.telegramdailybot.model.Notification;
import com.example.telegramdailybot.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationSchedulerService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramDailyBot.class);
    private final NotificationManagementController notificationManagementController;
    private final UserManagementController userManagementController;
    private final ChatManagementController chatManagementController;
    private final TelegramDailyBotProperties properties;
    private final TelegramDailyBot telegramDailyBot;

    @Autowired
    public NotificationSchedulerService(NotificationManagementController notificationManagementController,
                                        UserManagementController userManagementController,
                                        ChatManagementController chatManagementController,
                                        TelegramDailyBotProperties properties,
                                        TelegramDailyBot telegramDailyBot) {
        this.notificationManagementController = notificationManagementController;
        this.userManagementController = userManagementController;
        this.chatManagementController = chatManagementController;
        this.properties = properties;
        this.telegramDailyBot = telegramDailyBot;
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

    private void sendChatMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setDisableWebPagePreview(true);
        try {
            telegramDailyBot.execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message to chat: {}", chatId, e);
        }
    }

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
}
