package com.example.telegramdailybot;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramDailyBotConfiguration {
    @Bean
    public TelegramBotsApi telegramBotsApi(ChatEditHandler chatEditHandler, ChatDeletionHandler chatDeletionHandler, NotificationEditHandler notificationEditHandler,NotificationDeletionHandler notificationDeletionHandler, UserEditHandler userEditHandler,
            UserDeletionHandler userDeletionHandler,
                                           TelegramDailyBotProperties properties,
                                           ChatRepository chatRepository,
                                           NotificationRepository notificationRepository,
                                           UserRepository userRepository) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(new TelegramDailyBot(chatEditHandler, chatDeletionHandler, notificationEditHandler, notificationDeletionHandler, userEditHandler,userDeletionHandler, properties, chatRepository, notificationRepository, userRepository));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return botsApi;
    }
}

