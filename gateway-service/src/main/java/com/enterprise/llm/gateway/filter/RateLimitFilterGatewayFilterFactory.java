package com.enterprise.llm.gateway.filter;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Gateway filter referenced as "RateLimitFilter" in application.yml.
 * Uses a Redis sliding-window counter keyed on X-User-ID (or client IP).
 * The limit and window are configurable via application properties.
 */
@Component
public class RateLimitFilterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<RateLimitFilterGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilterGatewayFilterFactory.class);
    private static final String KEY_PREFIX   = "rate:";
    private static final int    WINDOW_SECS  = 60;
    private static final int    DEFAULT_LIMIT = 60;

    private final ReactiveStringRedisTemplate redis;

    public RateLimitFilterGatewayFilterFactory(ReactiveStringRedisTemplate redis) {
        super(Config.class);
        this.redis = redis;
    }

    @Override
    public GatewayFilter apply(Config config) {
        int limit = config.limit > 0 ? config.limit : DEFAULT_LIMIT;

        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            String clientIp = exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
            String key = KEY_PREFIX + (userId != null ? userId : clientIp);

            return redis.opsForValue().increment(key)
                    .flatMap(count -> {
                        if (count == 1) {
                            // First request in window — set expiry
                            redis.expire(key, Duration.ofSeconds(WINDOW_SECS)).subscribe();
                        }
                        if (count > limit) {
                            logger.warn("Rate limit exceeded: key={}, count={}", key, count);
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            exchange.getResponse().getHeaders().set("Retry-After",
                                    String.valueOf(WINDOW_SECS));
                            return exchange.getResponse().setComplete();
                        }
                        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining",
                                String.valueOf(limit - count));
                        return chain.filter(exchange);
                    });
        };
    }

    public static class Config {
        private int limit = DEFAULT_LIMIT;
        public int getLimit()        { return limit; }
        public void setLimit(int l)  { this.limit = l; }
    }
}
