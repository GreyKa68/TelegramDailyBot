package com.example.telegramdailybot.model;

import com.example.telegramdailybot.util.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnTransformer;

import java.time.ZonedDateTime;

@Data
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "text", length = 2000)
    private String text;

    @Column(name = "datetime")
    private ZonedDateTime datetime;

    @Column(name = "datetimexcluded", columnDefinition = "jsonb")
    @ColumnTransformer(read = "datetimexcluded", write = "CAST(? AS jsonb)")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode datetimexcluded;

    //Foreign key to chat.telegramchatid
    @Column(name = "chatid")
    private Long chatid;

    @Column(name = "repetition")
    private String repetition;
}