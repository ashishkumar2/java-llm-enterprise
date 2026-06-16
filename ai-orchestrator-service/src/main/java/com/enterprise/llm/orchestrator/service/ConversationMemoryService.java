package com.enterprise.llm.orchestrator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConversationMemoryService {
    private static final Logger logger = LoggerFactory.getLogger(ConversationMemoryService.class);
    private static final String KEY_PREFIX = "chat:history:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxMessages;
    private final long ttlHours;

    public ConversationMemoryService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.memory.max-messages:20}") int maxMessages,
            @Value("${app.memory.ttl-hours:24}") long ttlHours
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper  = objectMapper;
        this.maxMessages   = maxMessages;
        this.ttlHours      = ttlHours;
    }

    public List<Message> loadHistory(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        return raw.stream()
                .map(this::toSpringAiMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void save(String sessionId, String role, String content) {
        String key = KEY_PREFIX + sessionId;
        try {
            String json = objectMapper.writeValueAsString(new StoredMessage(role, content));
            redisTemplate.opsForList().rightPush(key, json);
            // Keep only the last maxMessages entries (sliding window)
            redisTemplate.opsForList().trim(key, -maxMessages, -1);
            redisTemplate.expire(key, Duration.ofHours(ttlHours));
        } catch (JsonProcessingException e) {
            logger.warn("Failed to persist message for sessionId={}: {}", sessionId, e.getMessage());
        }
    }

    private Message toSpringAiMessage(String json) {
        try {
            StoredMessage stored = objectMapper.readValue(json, StoredMessage.class);
            return switch (stored.role()) {
                case "user"      -> new UserMessage(stored.content());
                case "assistant" -> new AssistantMessage(stored.content());
                case "system"    -> new SystemMessage(stored.content());
                default -> {
                    logger.warn("Unknown message role '{}', skipping", stored.role());
                    yield null;
                }
            };
        } catch (JsonProcessingException e) {
            logger.warn("Failed to deserialize message from Redis: {}", e.getMessage());
            return null;
        }
    }

    public record StoredMessage(String role, String content) {}
}
