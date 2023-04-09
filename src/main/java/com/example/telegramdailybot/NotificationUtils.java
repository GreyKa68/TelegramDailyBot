package com.example.telegramdailybot;

import com.example.telegramdailybot.model.Notification;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationUtils {
    public static Notification parseNotificationText(String text) {
        Pattern notificationPattern = Pattern.compile("Текст уведомления: (.+?)\\nДата и время: (.+?)\\nЧастота: (.+?)\\nИсключения:(.*)", Pattern.DOTALL);
        Matcher matcher = notificationPattern.matcher(text);

        if (matcher.find()) {
            String notificationText = matcher.group(1).trim();
            String dateTimeStr = matcher.group(2).trim();
            String frequency = matcher.group(3).trim();
            String exclusionsText = matcher.group(4).trim();

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, dateTimeFormatter);

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

            return notification;
        } else {
            return null;
        }
    }
}
