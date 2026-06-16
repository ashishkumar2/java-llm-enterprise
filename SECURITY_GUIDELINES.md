# Security Guidelines

## Authentication & Authorization

### JWT Token Management
- Token format: RS256 signed
- Claims: `sub` (user ID), `roles`, `iat`, `exp`
- Expiration: 24 hours
- Refresh tokens: 30 days expiration
- Revocation via blacklist (Redis, 24h TTL)

```java
@Bean
public JwtAuthenticationProvider jwtProvider() {
    JwtAuthenticationProvider provider = new JwtAuthenticationProvider();
    provider.setJwtSecret(jwtSecret);
    provider.setTokenExpiry(Duration.ofHours(24));
    return provider;
}
```

### Role-Based Access Control (RBAC)

```java
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authz -> authz
            .requestMatchers(PUBLIC_PATHS).permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/user/**").hasRole("USER")
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

## Data Protection

### Encryption at Rest
- Database: Encrypted columns via pgcrypto or external KMS
- S3: AES-256 encryption
- Redis: Encrypted connections (TLS)
- Backups: Encrypted snapshots

### Encryption in Transit
- TLS 1.3 for all external communication
- Certificate pinning for critical services
- HTTPS enforcement

```java
@Configuration
public class HttpClientConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .interceptors((request, body, execution) -> {
                request.getHeaders().add("X-API-Version", "1.0");
                return execution.execute(request, body);
            })
            .build();
    }
}
```

### Secret Management

```java
@Configuration
public class SecretsConfig {
    
    @Bean
    public SecretsManagerClient secretsClient() {
        return SecretsManagerClient.builder()
            .region(Region.US_EAST_1)
            .build();
    }
    
    @Bean
    public String openAiApiKey(SecretsManagerClient client) {
        return getSecret(client, "openai-api-key");
    }
}
```

Never:
```java
// ❌ WRONG - Don't do this
@Value("${OPENAI_API_KEY}")
private String apiKey;

// ❌ WRONG - Don't hardcode
String apiKey = "sk-...";
```

## Input Validation & Sanitization

### Validate All User Input

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req) {
        // Input is automatically validated
        return ResponseEntity.ok(chatService.process(req));
    }
}

@Data
public class ChatRequest {
    @NotBlank(message = "Message cannot be blank")
    @Size(min = 1, max = 5000, message = "Message must be 1-5000 characters")
    private String message;
    
    @NotNull
    @Positive
    private Long sessionId;
}
```

### SQL Injection Prevention

```java
// ✅ GOOD - Use parameterized queries
@Query("SELECT c FROM Chat c WHERE c.userId = :userId AND c.id = :chatId")
Chat findByChatIdAndUser(@Param("userId") Long userId, @Param("chatId") Long chatId);

// ❌ BAD - String concatenation
String query = "SELECT * FROM chats WHERE user_id = " + userId;
```

## Threat Protection

### CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "https://app.example.com",
                        "https://staging.example.com"
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("Authorization", "Content-Type")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
```

### Rate Limiting

```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public RateLimitingFilter rateLimitFilter() {
        return new RateLimitingFilter(
            maxRequestsPerMinute(60),
            keyGenerator(request -> getUserId(request)),
            redisTemplate()
        );
    }
}
```

### CSRF Protection

```java
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers("/api/public/**")
        );
        return http.build();
    }
}
```

## Logging Security

### PII Masking

```java
@Aspect
@Component
public class PiiMaskingAspect {
    
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("\\b[\\w.-]+@[\\w.-]+\\.\\w+\\b");
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("\\b\\d{3}-\\d{3}-\\d{4}\\b");
    private static final Pattern SSN_PATTERN = 
        Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    
    public String maskPii(String input) {
        String result = input;
        result = EMAIL_PATTERN.matcher(result)
            .replaceAll("***@***.***");
        result = PHONE_PATTERN.matcher(result)
            .replaceAll("***-***-****");
        result = SSN_PATTERN.matcher(result)
            .replaceAll("***-**-****");
        return result;
    }
}
```

### Structured Logging with Context

```java
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    
    public ChatResponse process(ChatRequest req) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        MDC.put("userId", req.getUserId());
        
        try {
            log.info("Processing chat", Map.of(
                "sessionId", req.getSessionId(),
                "messageLength", req.getMessage().length()
            ));
            
            ChatResponse response = doProcess(req);
            
            log.info("Chat processed successfully");
            return response;
        } catch (Exception e) {
            log.error("Chat processing failed", Map.of(
                "errorCode", "CHAT_ERROR",
                "errorMessage", maskPii(e.getMessage())
            ));
            throw e;
        } finally {
            MDC.clear();
        }
    }
}
```

## Audit Logging

### Track All Sensitive Operations

```java
@Component
public class AuditService {
    
    public void logAiRequest(ChatRequest req, String userId, String action) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setResourceType("CHAT");
        log.setTimestamp(LocalDateTime.now());
        log.setIpAddress(getClientIp());
        log.setDetails(Map.of(
            "modelUsed", "gpt-4",
            "tokensUsed", getTokenCount(req),
            "cost", calculateCost(req)
        ));
        log.setStatus("SUCCESS");
        
        auditRepository.save(log);
    }
}
```

## API Security

### API Key Management

```java
@Component
public class ApiKeyValidator {
    
    @Bean
    public HandlerInterceptor apiKeyInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    Object handler) {
                String apiKey = request.getHeader("X-API-Key");
                if (isValidApiKey(apiKey)) {
                    return true;
                }
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
        };
    }
}
```

### Request Signing (for webhooks)

```java
public class WebhookValidator {
    
    public boolean validateSignature(String payload, String signature) {
        String computed = HmacUtils.hmacSha256Hex(SECRET, payload);
        return MessageDigest.isEqual(
            computed.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
```

## Dependency Security

### Maven Dependency Management

```xml
<!-- Check for vulnerabilities -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>8.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>

<!-- Enforce version policies -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <id>enforce-versions</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <bannedDependencies>
                        <searchTransitive>true</searchTransitive>
                        <excludes>
                            <!-- Old/vulnerable versions -->
                        </excludes>
                    </bannedDependencies>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Security Testing

### Unit Test Template

```java
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceSecurityTest {
    
    @Test
    void testPasswordHashing_givenPlaintext_returnsHashedPassword() {
        String password = "mySecurePassword123";
        String hashed = authService.hashPassword(password);
        
        assertNotEquals(password, hashed);
        assertTrue(authService.verifyPassword(password, hashed));
    }
    
    @Test
    void testApiKeyGeneration_givenUser_returnsSecureKey() {
        String apiKey = authService.generateApiKey();
        
        assertEquals(32, apiKey.length());
        assertTrue(apiKey.matches("[a-zA-Z0-9]{32}"));
    }
    
    @Test
    void testTokenExpiration_givenExpiredToken_throwsException() {
        String expiredToken = createExpiredToken();
        
        assertThrows(JwtException.class, () -> 
            authService.validateToken(expiredToken)
        );
    }
}
```

## Incident Response

### Breach Protocol
1. **Detect**: Monitor anomalies and alerts
2. **Isolate**: Disable affected credentials
3. **Assess**: Determine scope and impact
4. **Notify**: Alert affected users within 24 hours
5. **Remediate**: Fix vulnerability and deploy patch
6. **Review**: Post-incident analysis

## Security Checklist

- [ ] All secrets in environment variables
- [ ] No secrets in code repository
- [ ] HTTPS enabled for all endpoints
- [ ] JWT tokens validated on every request
- [ ] Input validation on all fields
- [ ] SQL injection prevented (parameterized queries)
- [ ] CORS properly configured
- [ ] Rate limiting enabled
- [ ] PII masked in logs
- [ ] Audit logging for sensitive operations
- [ ] Dependencies scanned for vulnerabilities
- [ ] Error messages don't leak information
- [ ] Database backups encrypted
- [ ] TLS 1.3 enforced
- [ ] Security headers configured

---

**Version**: 1.0
**Last Updated**: June 2026
**Status**: Active
