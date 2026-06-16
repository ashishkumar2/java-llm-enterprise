package com.enterprise.llm.gateway.filter;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that ensures every request carries a unique X-Request-ID,
 * generating one if the caller did not supply it. The ID is echoed back in
 * the response header for client-side correlation.
 */
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    static final String REQUEST_ID_HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        final String id = requestId;
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(REQUEST_ID_HEADER, id)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build())
                .then(Mono.fromRunnable(() ->
                        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, id)));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
