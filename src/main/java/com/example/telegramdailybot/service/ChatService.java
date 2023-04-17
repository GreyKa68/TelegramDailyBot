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

    public void delete(Chat chat) {
        chatRepository.delete(chat);
    }

    public Optional<Chat> findById(long chatId) {
        return chatRepository.findById(chatId);
    }

    public List<Chat> findAll() {
        return chatRepository.findAll();
    }

    public void addChatsFromText(String text) {
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

                save(chat);
            }
        }
    }

    public void deleteChatsFromText(String text) {
        String[] lines = text.split("\\n");

        for (String line : lines) {
            findById(Long.parseLong(line)).ifPresent(this::delete
            );
        }
    }

    public void editChatsFromText(String text) {
        String[] lines = text.split("\\n");

        for (String line : lines) {
            String[] parts = line.split(",", 3);
            if (parts.length == 3) {
                Long telegramid = Long.parseLong(parts[0].trim());
                String name = parts[1].trim();
                String role = parts[2].trim();

                Chat chat = findById(telegramid).orElse(null);

                if (chat != null) {
                    chat.setName(name);
                    chat.setRole(role);
                    save(chat);
                }
            }
        }
    }

    public boolean isAdmin(long chatId) {
        Optional<Chat> chatOptional = findById(chatId);
        return chatOptional.isPresent() && "admin".equals(chatOptional.get().getRole());
    }

    public String generateChatList() {
        List<Chat> chats = findAll();
        StringBuilder sb = new StringBuilder("Список чатов:\n\n");
        for (Chat chat : chats) {
            sb.append(chat.getTelegramchatid()).append(", ")
                    .append(chat.getName()).append(", ").append(chat.getRole()).append('\n');
        }
        return sb.toString();
    }
}
