package com.example.telegramdailybot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "chats")
public class Chat {
    private String name;
    @Id
    private Long telegramchatid;
    private String role;
}
