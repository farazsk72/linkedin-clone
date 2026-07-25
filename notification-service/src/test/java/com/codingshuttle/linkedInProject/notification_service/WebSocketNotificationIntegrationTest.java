package com.codingshuttle.linkedInProject.notification_service;

import com.codingshuttle.linkedInProject.notification_service.entity.Notification;
import com.codingshuttle.linkedInProject.notification_service.service.NotificationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import javax.crypto.SecretKey;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Boots the real notification-service context (Kafka and Eureka excluded, H2 in
 * place of Postgres) and drives it with an actual STOMP client, proving the
 * pieces the unit tests cannot: the endpoint is reachable, the CONNECT auth
 * interceptor is wired onto the inbound channel, and a saved notification is
 * delivered over the wire to the right user's queue - and only to an
 * authenticated session.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
                "spring.datasource.url=jdbc:h2:mem:notif;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false",
                "management.tracing.enabled=false",
                // Non-browser client sends no Origin; accept any so it is not the
                // variable under test here.
                "cors.allowed-origin=*",
                "jwt.secretKey=integration-test-secret-key-long-enough-for-hs-signing-123456",
        }
)
class WebSocketNotificationIntegrationTest {

    private static final String SECRET =
            "integration-test-secret-key-long-enough-for-hs-signing-123456";

    @LocalServerPort
    private int port;

    @Autowired
    private NotificationService notificationService;

    private String wsUrl() {
        // Context-path /notifications + the /ws STOMP endpoint. http scheme
        // because the client is a SockJS client, matching withSockJS().
        return "http://localhost:" + port + "/notifications/ws";
    }

    private String token(String subject) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 60_000))
                .signWith(key)
                .compact();
    }

    private WebSocketStompClient stompClient() {
        WebSocketStompClient client = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        // JSR-310-aware mapper, so the notification's LocalDateTime createdAt
        // deserializes on the client the same way the server serialized it.
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(Jackson2ObjectMapperBuilder.json().build());
        client.setMessageConverter(converter);
        return client;
    }

    private StompSession connect(String bearer) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        if (bearer != null) {
            connectHeaders.add("Authorization", bearer);
        }
        return stompClient()
                .connectAsync(wsUrl(), new WebSocketHttpHeaders(), connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    @Test
    void deliversASavedNotificationToTheOwnersQueue() throws Exception {
        StompSession session = connect("Bearer " + token("42"));

        BlockingQueue<Notification> received = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/notifications", new StompSessionHandlerAdapter() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return Notification.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                received.add((Notification) payload);
            }
        });
        // Let the SUBSCRIBE register before the push is triggered.
        TimeUnit.MILLISECONDS.sleep(400);

        notificationService.addNotification(Notification.builder()
                .userId(42L)
                .type("POST_LIKED")
                .message("User 3 liked your post")
                .build());

        Notification delivered = received.poll(5, TimeUnit.SECONDS);
        assertThat(delivered).isNotNull();
        assertThat(delivered.getMessage()).isEqualTo("User 3 liked your post");
        assertThat(delivered.getUserId()).isEqualTo(42L);

        session.disconnect();
    }

    @Test
    void doesNotDeliverToADifferentUsersQueue() throws Exception {
        // Connected as user 99; a notification addressed to user 42 must not
        // arrive here - convertAndSendToUser routes by the CONNECT Principal.
        StompSession session = connect("Bearer " + token("99"));

        BlockingQueue<Notification> received = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/notifications", new StompSessionHandlerAdapter() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return Notification.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                received.add((Notification) payload);
            }
        });
        TimeUnit.MILLISECONDS.sleep(400);

        notificationService.addNotification(Notification.builder()
                .userId(42L)
                .type("POST_LIKED")
                .message("not for user 99")
                .build());

        assertThat(received.poll(2, TimeUnit.SECONDS)).isNull();

        session.disconnect();
    }

    @Test
    void rejectsAConnectWithoutAToken() {
        // The auth interceptor throws on CONNECT, so the handshake never completes.
        assertThatThrownBy(() -> connect(null))
                .isInstanceOf(ExecutionException.class);
    }
}
