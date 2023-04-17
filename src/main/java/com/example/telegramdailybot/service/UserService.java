package com.example.telegramdailybot.service;

import com.example.telegramdailybot.model.User;
import com.example.telegramdailybot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ChatService chatService;

    @Autowired
    public UserService(UserRepository userRepository, ChatService chatService) {

        this.userRepository = userRepository;
        this.chatService = chatService;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void addUsersFromText(String text, long chatId) {
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

                save(user);
            }
        }
    }

    @Transactional
    public void deleteUsersFromText(String text, long chatId, long userId) {
        String[] lines = text.split("\\n");

        for (String line : lines) {
            int userIdtoDelete = Integer.parseInt(line);
            User user = findById(userIdtoDelete).orElse(null);

            if (user != null && (chatService.isAdmin(userId) || userId == chatId)) {
                deleteById(userIdtoDelete);
            }
        }
    }

    @Transactional
    public void editUsersFromText(String text, long chatId, long userId) {
        String[] lines = text.split("\\n");

        for (String line : lines) {
            String[] parts = line.split(",", 3);
            if (parts.length == 3) {
                int id = Integer.parseInt(parts[0].trim());
                String name = parts[1].trim();
                String username = parts[2].trim().replace("@", "");

                User user = findById(id).orElse(null);

                if (user != null && (chatService.isAdmin(userId) || userId == chatId)) {
                    user.setName(name);
                    user.setUsername(username);
                    save(user);
                }
            }
        }
    }

    public void deleteById(int id) {
        userRepository.deleteById(id);
    }

    public Optional<User> findById(int id) {
        return userRepository.findById(id);
    }

    public List<User> findByChatid(long chatId) {
        return userRepository.findByChatid(chatId);
    }

    public void resetWinners(long chatId) {
        List<User> winners = findByChatid(chatId);
        winners.forEach(user -> {
            user.setHaswon(false);
            save(user);
        });
    }

    public User findWinner(Long chatId) {
        List<User> users = findByChatid(chatId);

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
                save(user);
            }
            usersWithoutWin.addAll(users);
        }

        Random random = new Random();
        int winnerIndex = random.nextInt(usersWithoutWin.size());
        User winner = usersWithoutWin.get(winnerIndex);
        winner.setHaswon(true);
        save(winner);

        return winner;
    }

    public String generateUserListMessage(Long chatId, List<String> fieldsToDisplay, Map<String, String> customHeaders) {
        List<User> users = findByChatid(chatId);
        if (users.isEmpty()) {
            return "Участники розыгрышей в этом чате отсутствуют";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Участники розыгрышей в этом чате:\n");

        // Add headers row
        for (String field : fieldsToDisplay) {
            sb.append("| ");
            String customHeader = customHeaders.getOrDefault(field, field);
            sb.append(customHeader);
            sb.append(" ");
        }
        sb.append("|\n");

        // Add values
        for (User user : users) {
            for (String field : fieldsToDisplay) {
                sb.append("| ");
                switch (field) {
                    case "id" -> sb.append(user.getId());
                    case "chatid" -> sb.append(user.getChatid());
                    case "name" -> sb.append(user.getName());
                    case "username" -> sb.append(user.getUsername());
                    case "haswon" -> sb.append(user.isHaswon() ? "Yes" : "No");
                }
                sb.append(" ");
            }
            sb.append("|\n");
        }

        return sb.toString();
    }

}
