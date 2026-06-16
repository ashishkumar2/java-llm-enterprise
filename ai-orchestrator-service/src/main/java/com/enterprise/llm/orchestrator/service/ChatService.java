package com.enterprise.llm.orchestrator.service;

import com.enterprise.llm.orchestrator.model.ChatRequest;
import com.enterprise.llm.orchestrator.model.ChatResponse;

public interface ChatService {
    ChatResponse processChat(ChatRequest request);
}
