package com.example.telegramdailybot.util;

import com.example.telegramdailybot.model.Notification;
import com.example.telegramdailybot.model.ParseResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.ZonedDateTime;

public class NotificationUtils {

    public static ParseResult parseNotificationText(String text) {
        Pattern notificationPattern = Pattern.compile("Текст уведомления:\\s?(.+?)\\nДата и время:\\s?(.+?)\\nЧастота:\\s?(.+?)\\nИсключения:(.*)", Pattern.DOTALL);
        Matcher matcher = notificationPattern.matcher(text);
        if (matcher.find()) {
            String notificationText = matcher.group(1).trim();
            String dateTimeStr = matcher.group(2).trim();
            String frequency = matcher.group(3).trim();
            String exclusionsText = matcher.group(4).trim();
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmZ");
            ZonedDateTime dateTime;
            try {
                dateTime = ZonedDateTime.parse(dateTimeStr, dateTimeFormatter);
            } catch (DateTimeParseException e) {
                return new ParseResult(null, "Ошибка при парсинге даты и времени: " + dateTimeStr);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode exclusionsJson = objectMapper.createObjectNode();
            Pattern weekendsPattern = Pattern.compile("- Исключить СБ и ВС");
            Matcher weekendsMatcher = weekendsPattern.matcher(exclusionsText);
            exclusionsJson.put("weekends", weekendsMatcher.find());
            Pattern skipDaysPattern = Pattern.compile("\\* (\\d{4}-\\d{2}-\\d{2}) \\(every (\\d+) days\\)");
            Matcher skipDaysMatcher = skipDaysPattern.matcher(exclusionsText);
            ArrayNode skipDaysArray = objectMapper.createArrayNode();
            while (skipDaysMatcher.find()) {
                String day = skipDaysMatcher.group(1);
                int skipFrequency = Integer.parseInt(skipDaysMatcher.group(2));
                ObjectNode skipDayNode = objectMapper.createObjectNode();
                skipDayNode.put("day", day);
                skipDayNode.put("frequency", skipFrequency);
                skipDaysArray.add(skipDayNode);
            }
            exclusionsJson.set("skip_days", skipDaysArray);
            Notification notification = new Notification();
            notification.setText(notificationText);
            notification.setDatetime(dateTime);
            notification.setRepetition(frequency);
            notification.setDatetimexcluded(exclusionsJson);
            return new ParseResult(notification, null);
        } else {
            return new ParseResult(null, "Пожалуйста, проверьте соответствие шаблону.");
        }
    }
}

