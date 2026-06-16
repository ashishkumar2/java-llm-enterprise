package com.enterprise.llm.orchestrator.service;

import com.enterprise.llm.orchestrator.model.ChatRequest;
import com.enterprise.llm.orchestrator.model.ChatResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultChatServiceTest {

    @Mock private ChatClient                chatClient;
    @Mock private ConversationMemoryService memoryService;
    @Mock private RagContextService         ragContextService;

    private DefaultChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new DefaultChatService(chatClient, memoryService, ragContextService, "test-model");

        // Stub common behaviour
        when(memoryService.loadHistory(anyString())).thenReturn(List.of());
        when(ragContextService.retrieveContext(anyString())).thenReturn("");

        AssistantMessage aiMsg = mock(AssistantMessage.class);
        when(aiMsg.getContent()).thenReturn("Hello from AI");

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(aiMsg);

        org.springframework.ai.chat.ChatResponse aiResponse = mock(org.springframework.ai.chat.ChatResponse.class);
        when(aiResponse.getResult()).thenReturn(generation);

        when(chatClient.call(any(Prompt.class))).thenReturn(aiResponse);
    }

    @Test
    void processChat_callsLlmAndReturnsResponse() {
        ChatRequest request = new ChatRequest("Hello", "session-1", "user-1", null);

        ChatResponse response = chatService.processChat(request);

        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(response.message()).isEqualTo("Hello from AI");
        assertThat(response.model()).isEqualTo("test-model");
        assertThat(response.tokenCount()).isPositive();
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void processChat_savesUserAndAssistantTurnToMemory() {
        ChatRequest request = new ChatRequest("Hello", "session-1", "user-1", null);

        chatService.processChat(request);

        verify(memoryService).save(eq("session-1"), eq("user"),      eq("Hello"));
        verify(memoryService).save(eq("session-1"), eq("assistant"), eq("Hello from AI"));
    }

    @Test
    void processChat_tokenCountIncludesBothInputAndOutput() {
        String longInput = "A".repeat(400);
        ChatRequest request = new ChatRequest(longInput, "session-2", "user-2", null);

        ChatResponse response = chatService.processChat(request);

        // 400-char input / 4 + output contribution
        assertThat(response.tokenCount()).isGreaterThan(100);
    }

    @Test
    void processChat_loadsHistoryForSession() {
        ChatRequest request = new ChatRequest("Hi", "session-3", "user-3", null);

        chatService.processChat(request);

        verify(memoryService).loadHistory("session-3");
    }
}
