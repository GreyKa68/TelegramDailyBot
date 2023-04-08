package com.example.telegramdailybot;

import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@Entity
@Table(name = "chats")
public class Chat {
    private String name;
    @Id
    private Long telegramchatid;
    private String role;
}
