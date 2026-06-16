package com.enterprise.llm.orchestrator.health;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("openAi")
@ConditionalOnProperty(name = "management.health.openai.enabled", havingValue = "true", matchIfMissing = true)
public class OpenAiHealthIndicator implements HealthIndicator {

    private final EmbeddingClient embeddingClient;

    public OpenAiHealthIndicator(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    @Override
    public Health health() {
        try {
            embeddingClient.embed("health-check");
            return Health.up().withDetail("provider", "openai").build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("provider", "openai")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
