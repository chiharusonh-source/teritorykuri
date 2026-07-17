package io.github.chiharusonh.territory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebSocketIntegrationTest {
    private static final Pattern ROOM_CODE = Pattern.compile("\\\"code\\\":\\\"(\\d{6})\\\"");

    @LocalServerPort
    private int port;

    @Test
    void twoClientsCanCreateJoinAndReceiveTheSameMove() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        MessageListener hostMessages = new MessageListener();
        MessageListener guestMessages = new MessageListener();
        URI endpoint = URI.create("ws://localhost:" + port + "/ws");

        WebSocket host = client.newWebSocketBuilder()
                .buildAsync(endpoint, hostMessages)
                .get(5, TimeUnit.SECONDS);
        WebSocket guest = client.newWebSocketBuilder()
                .buildAsync(endpoint, guestMessages)
                .get(5, TimeUnit.SECONDS);

        try {
            host.sendText("{\"type\":\"create\"}", true).join();
            String created = hostMessages.next();
            Matcher codeMatcher = ROOM_CODE.matcher(created);
            assertThat(created).contains("\"type\":\"created\"");
            assertThat(codeMatcher.find()).isTrue();

            String code = codeMatcher.group(1);
            guest.sendText("{\"type\":\"join\",\"code\":\"" + code + "\"}", true).join();
            assertThat(hostMessages.next()).contains("\"type\":\"started\"", "\"role\":1");
            assertThat(guestMessages.next()).contains("\"type\":\"started\"", "\"role\":2");

            host.sendText("{\"type\":\"move\",\"vertex\":10}", true).join();
            String hostState = hostMessages.next();
            String guestState = guestMessages.next();

            assertThat(hostState).contains("\"type\":\"state\"", "\"ply\":1");
            assertThat(guestState).isEqualTo(hostState);
        } finally {
            host.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").join();
            guest.sendClose(WebSocket.NORMAL_CLOSURE, "test complete").join();
        }
    }

    private static final class MessageListener implements WebSocket.Listener {
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final StringBuilder fragments = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            synchronized (fragments) {
                fragments.append(data);
                if (last) {
                    messages.add(fragments.toString());
                    fragments.setLength(0);
                }
            }
            webSocket.request(1);
            return null;
        }

        String next() throws InterruptedException {
            String message = messages.poll(5, TimeUnit.SECONDS);
            assertThat(message).as("WebSocket response").isNotNull();
            return message;
        }
    }
}
