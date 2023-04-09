package com.example.telegramdailybot.service;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ChatGPT3Service {

    private final OpenAiService openAiService;
    private final int maxTokens;

    public ChatGPT3Service(
            @Value("${openai.token}") String openAiToken,
            @Value("${openai.timeout}") long timeoutSeconds,
            @Value("${openai.maxTokens}") int maxTokens) {
        this.openAiService = new OpenAiService(openAiToken, Duration.ofSeconds(timeoutSeconds));
        this.maxTokens = maxTokens;
    }

    @Async
    public CompletableFuture<String> chat(String inputText) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage("user", inputText));

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo") // You can choose another model if you want
                .messages(chatMessages)
                .maxTokens(maxTokens)
                .build();
        return CompletableFuture.supplyAsync(() ->
                openAiService.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage().getContent()
        );
    }
}

