package com.example.telegramdailybot;


import com.example.telegramdailybot.handler.*;
import com.example.telegramdailybot.repository.ChatRepository;
import com.example.telegramdailybot.repository.NotificationRepository;
import com.example.telegramdailybot.repository.UserRepository;
import com.example.telegramdailybot.service.ChatGPT3Service;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramDailyBotConfiguration {
    @Bean
    public TelegramBotsApi telegramBotsApi(ChatGPT3Service chatGpt3Service, ChatEditHandler chatEditHandler, ChatDeletionHandler chatDeletionHandler, NotificationEditHandler notificationEditHandler, NotificationDeletionHandler notificationDeletionHandler, UserEditHandler userEditHandler,
                                           UserDeletionHandler userDeletionHandler,
                                           TelegramDailyBotProperties properties,
                                           ChatRepository chatRepository,
                                           NotificationRepository notificationRepository,
                                           UserRepository userRepository) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(new TelegramDailyBot(chatGpt3Service, chatEditHandler, chatDeletionHandler, notificationEditHandler, notificationDeletionHandler, userEditHandler, userDeletionHandler, properties, chatRepository, notificationRepository, userRepository));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return botsApi;
    }
}

