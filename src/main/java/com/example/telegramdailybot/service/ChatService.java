package com.example.telegramdailybot.service;

import com.example.telegramdailybot.model.Chat;
import com.example.telegramdailybot.repository.ChatRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public Optional<Chat> findById(long chatId) {
        return chatRepository.findById(chatId);
    }

    public List<Chat> findAll() {
        return chatRepository.findAll();
    }

    public boolean isAdmin(long chatId) {
        Optional<Chat> chatOptional = findById(chatId);
        return chatOptional.isPresent() && "admin".equals(chatOptional.get().getRole());
    }
}
