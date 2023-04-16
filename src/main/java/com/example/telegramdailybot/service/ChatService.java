package com.example.telegramdailybot.service;

import com.example.telegramdailybot.model.Chat;
import com.example.telegramdailybot.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatRepository chatRepository;

    @Autowired
    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    public Chat save(Chat chat) {
        return chatRepository.save(chat);
    }

}
