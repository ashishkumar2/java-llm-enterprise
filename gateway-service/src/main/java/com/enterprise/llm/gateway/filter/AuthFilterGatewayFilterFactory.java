package com.enterprise.llm.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Gateway filter referenced as "AuthFilter" in application.yml.
 * Validates that the request carries a JWT (handled by Spring Security's
 * OAuth2 resource server) and propagates the subject claim as X-User-ID
 * so downstream services can identify the caller without re-validating the token.
 */
@Component
public class AuthFilterGatewayFilterFactory
        extends AbstractGatewayFilterFactory<AuthFilterGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(AuthFilterGatewayFilterFactory.class);

    public AuthFilterGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) ->
                ReactiveSecurityContextHolder.getContext()
                        .flatMap(ctx -> {
                            if (ctx.getAuthentication() == null ||
                                    !ctx.getAuthentication().isAuthenticated()) {
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return exchange.getResponse().setComplete();
                            }
                            Object principal = ctx.getAuthentication().getPrincipal();
                            String userId = (principal instanceof Jwt jwt)
                                    ? jwt.getSubject()
                                    : ctx.getAuthentication().getName();

                            logger.debug("Authenticated request: userId={}", userId);
                            var mutated = exchange.getRequest().mutate()
                                    .header("X-User-ID", userId)
                                    .build();
                            return chain.filter(exchange.mutate().request(mutated).build());
                        })
                        .switchIfEmpty(chain.filter(exchange)); // permit unauthenticated if security allows
    }

    public static class Config {}
}
