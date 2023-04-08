package com.example.telegramdailybot;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(name = "username")
    private String username;

    //Foreign key to chat.telegramchatid
    @Column(name = "chatid")
    private Long chatid;

    @Column(name = "haswon")
    private boolean haswon;
}
