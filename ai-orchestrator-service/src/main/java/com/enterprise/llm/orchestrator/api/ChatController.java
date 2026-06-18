package com.enterprise.llm.orchestrator.api;

import com.enterprise.llm.orchestrator.model.ChatRequest;
import com.enterprise.llm.orchestrator.model.ChatResponse;
import com.enterprise.llm.orchestrator.service.ChatService;
import com.enterprise.llm.orchestrator.service.ConversationMemoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final ConversationMemoryService memoryService;

    public ChatController(ChatService chatService, ConversationMemoryService memoryService) {
        this.chatService   = chatService;
        this.memoryService = memoryService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.processChat(request));
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ConversationMemoryService.StoredMessage>> history(
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(memoryService.getStoredHistory(sessionId));
    }
}
