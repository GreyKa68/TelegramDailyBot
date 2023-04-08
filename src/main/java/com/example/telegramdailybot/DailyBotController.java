package com.example.telegramdailybot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api")
public class DailyBotController {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // Chat management
    @GetMapping("/chats")
    public List<Chat> getAllChats() {
        return chatRepository.findAll();
    }

    @PostMapping("/chats")
    public Chat createChat(@RequestBody Chat chat) {
        return chatRepository.save(chat);
    }

    // Delete chat
    @DeleteMapping("/chats/{id}")
    public void deleteChat(@PathVariable Long id) {
        chatRepository.deleteById(id);
    }

    // User management
    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("/users")
    public User createUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    // Delete user
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable Integer id) {
        userRepository.deleteById(id);
    }

    // Notification management
    @GetMapping("/notifications")
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @PostMapping("/notifications")
    public Notification createNotification(@RequestBody Notification notification) {
        return notificationRepository.save(notification);
    }

    // Delete notification
    @DeleteMapping("/notifications/{id}")
    public void deleteNotification(@PathVariable Integer id) {
        notificationRepository.deleteById(id);
    }
}

