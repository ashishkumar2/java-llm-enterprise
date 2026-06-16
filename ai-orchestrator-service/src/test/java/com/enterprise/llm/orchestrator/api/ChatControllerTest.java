package com.enterprise.llm.orchestrator.api;

import com.enterprise.llm.orchestrator.model.ChatRequest;
import com.enterprise.llm.orchestrator.model.ChatResponse;
import com.enterprise.llm.orchestrator.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@WithMockUser
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void chat_givenValidRequest_returns200WithResponse() throws Exception {
        ChatResponse stubResponse = new ChatResponse("session-1", "AI response", "gpt-4", 50, Instant.now());
        when(chatService.processChat(any())).thenReturn(stubResponse);

        String body = objectMapper.writeValueAsString(new ChatRequest("Hello", "session-1", "user-1", null));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                .andExpect(jsonPath("$.message").value("AI response"))
                .andExpect(jsonPath("$.model").value("gpt-4"))
                .andExpect(jsonPath("$.tokenCount").value(50));
    }

    @Test
    void chat_givenBlankMessage_returns400WithDetails() throws Exception {
        String body = objectMapper.writeValueAsString(new ChatRequest("", "session-1", "user-1", null));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void chat_givenBlankSessionId_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new ChatRequest("Hello", "", "user-1", null));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void chat_givenMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void chat_givenMessageExceedingLimit_returns400() throws Exception {
        String tooLong = "x".repeat(5001);
        String body = objectMapper.writeValueAsString(new ChatRequest(tooLong, "session-1", "user-1", null));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
