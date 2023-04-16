package com.example.telegramdailybot.service;

import com.example.telegramdailybot.model.User;
import com.example.telegramdailybot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User user) {
        return userRepository.save(user);
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

    public String findWinner(Long chatId) {
        List<User> users = findByChatid(chatId);

        if (users.isEmpty()) {
            return "Участники для розыгрыша в этом чате отсутствуют";
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

        return "Участник " + winner.getName() + ", @" + winner.getUsername() + " выиграл!";
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
