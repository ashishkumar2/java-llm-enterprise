package com.enterprise.llm.orchestrator.service;

import com.enterprise.llm.common.exception.AiProcessingException;
import com.enterprise.llm.orchestrator.model.ChatRequest;
import com.enterprise.llm.orchestrator.model.ChatResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DefaultChatService implements ChatService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultChatService.class);

    private final ChatClient               chatClient;
    private final ConversationMemoryService memoryService;
    private final RagContextService         ragContextService;
    private final String                    modelName;

    public DefaultChatService(
            ChatClient chatClient,
            ConversationMemoryService memoryService,
            RagContextService ragContextService,
            @Value("${spring.ai.openai.model:gpt-4}") String modelName
    ) {
        this.chatClient       = chatClient;
        this.memoryService    = memoryService;
        this.ragContextService = ragContextService;
        this.modelName        = modelName;
    }

    @Override
    public ChatResponse processChat(ChatRequest request) {
        logger.info("Processing chat: sessionId={}, messageLength={}",
                request.sessionId(), request.message().length());
        try {
            // 1. Load prior conversation turns from Redis
            List<Message> history = memoryService.loadHistory(request.sessionId());

            // 2. Retrieve relevant document chunks from pgvector
            String ragContext = ragContextService.retrieveContext(request.message());

            // 3. Assemble the full prompt
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(buildSystemPrompt(ragContext)));
            messages.addAll(history);
            messages.add(new UserMessage(request.message()));

            // 4. Call the LLM
            org.springframework.ai.chat.ChatResponse aiResponse =
                    chatClient.call(new Prompt(messages));
            String responseText = aiResponse.getResult().getOutput().getContent();

            // 5. Persist both turns to Redis for next request
            memoryService.save(request.sessionId(), "user",      request.message());
            memoryService.save(request.sessionId(), "assistant", responseText);

            int tokens = estimateTokens(request.message(), responseText);
            logger.info("Chat complete: sessionId={}, model={}, estimatedTokens={}",
                    request.sessionId(), modelName, tokens);

            return new ChatResponse(request.sessionId(), responseText, modelName, tokens, Instant.now());

        } catch (AiProcessingException e) {
            throw e;
        } catch (Exception e) {
            logger.error("LLM call failed: sessionId={}", request.sessionId(), e);
            throw new AiProcessingException("Failed to process chat request: " + e.getMessage(), e);
        }
    }

    private String buildSystemPrompt(String ragContext) {
        StringBuilder sb = new StringBuilder(
                "You are a helpful enterprise AI assistant. " +
                "Be concise, accurate, and professional.");
        if (ragContext != null && !ragContext.isBlank()) {
            sb.append("\n\nRelevant context from the knowledge base:\n").append(ragContext);
        }
        return sb.toString();
    }

    // Approximate: ~4 characters per token for English text
    private int estimateTokens(String input, String output) {
        return Math.max(1, (input.length() + output.length()) / 4);
    }
}
