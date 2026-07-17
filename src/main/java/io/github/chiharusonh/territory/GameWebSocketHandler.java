package io.github.chiharusonh.territory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.json.JsonMapper;

@Component
final class GameWebSocketHandler extends TextWebSocketHandler {
    private final RoomService rooms;
    private final JsonMapper jsonMapper;

    GameWebSocketHandler(RoomService rooms, JsonMapper jsonMapper) {
        this.rooms = rooms;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Map<String, Object> request;
        try {
            Object decoded = jsonMapper.readValue(message.getPayload(), Object.class);
            if (!(decoded instanceof Map<?, ?> decodedMap)) {
                sendError(session, "通信内容を読み取れませんでした。");
                return;
            }
            Map<String, Object> normalized = new HashMap<>();
            decodedMap.forEach((key, value) -> normalized.put(String.valueOf(key), value));
            request = normalized;
        } catch (Exception exception) {
            sendError(session, "通信内容を読み取れませんでした。");
            return;
        }

        String type = String.valueOf(request.getOrDefault("type", ""));
        switch (type) {
            case "create" -> createRoom(session);
            case "join" -> joinRoom(session, String.valueOf(request.getOrDefault("code", "")));
            case "move" -> move(session, numberValue(request.get("vertex")));
            case "disconnect" -> disconnect(session);
            default -> sendError(session, "未対応の操作です。");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        notifyDisconnected(rooms.leave(session).orElse(null), session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws IOException {
        notifyDisconnected(rooms.leave(session).orElse(null), session);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private void createRoom(WebSocketSession session) throws IOException {
        try {
            RoomService.RoomView room = rooms.create(session);
            send(session, Map.of(
                    "type", "created",
                    "code", room.code(),
                    "role", 1,
                    "state", stateView(room.game())));
        } catch (IllegalStateException exception) {
            sendError(session, exception.getMessage());
        }
    }

    private void joinRoom(WebSocketSession session, String code) throws IOException {
        if (!code.matches("\\d{6}")) {
            sendError(session, "6桁の招待コードを入力してください。");
            return;
        }
        RoomService.RoomView room = rooms.join(code, session).orElse(null);
        if (room == null) {
            sendError(session, "部屋が見つからないか、すでに満員です。");
            return;
        }
        send(room.host(), Map.of(
                "type", "started",
                "code", code,
                "role", 1,
                "state", stateView(room.game())));
        send(room.guest(), Map.of(
                "type", "started",
                "code", code,
                "role", 2,
                "state", stateView(room.game())));
    }

    private void move(WebSocketSession session, Integer vertex) throws IOException {
        if (vertex == null) {
            sendError(session, "置く交点を指定してください。");
            return;
        }
        RoomService.MoveOutcome outcome = rooms.move(session, vertex).orElse(null);
        if (outcome == null) {
            sendError(session, "対戦相手との接続が完了していません。");
            return;
        }
        if (!outcome.result().accepted()) {
            sendError(session, outcome.result().message());
            return;
        }
        Map<String, Object> response = Map.of(
                "type", "state",
                "state", stateView(outcome.room().game()));
        send(outcome.room().host(), response);
        send(outcome.room().guest(), response);
    }

    private void disconnect(WebSocketSession session) throws IOException {
        notifyDisconnected(rooms.leave(session).orElse(null), session);
        if (session.isOpen()) {
            session.close(CloseStatus.NORMAL);
        }
    }

    private void notifyDisconnected(RoomService.RoomView room, WebSocketSession source) throws IOException {
        if (room == null) {
            return;
        }
        WebSocketSession other = source.getId().equals(room.host().getId()) ? room.guest() : room.host();
        if (other != null && other.isOpen()) {
            send(other, Map.of(
                    "type", "disconnected",
                    "message", "相手との接続が切れました。"));
        }
    }

    private Map<String, Object> stateView(GameState state) {
        return Map.ofEntries(
                Map.entry("stones", state.stones),
                Map.entry("owners", state.owners),
                Map.entry("turn", state.turn),
                Map.entry("cornersUsed", Map.of("1", state.cornersUsed[1], "2", state.cornersUsed[2])),
                Map.entry("ply", state.ply),
                Map.entry("over", state.over),
                Map.entry("winner", state.winner),
                Map.entry("winReason", state.winReason),
                Map.entry("status", state.status),
                Map.entry("blueScore", new GameEngine().countOf(state, 1)),
                Map.entry("redScore", new GameEngine().countOf(state, 2)));
    }

    private Integer numberValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        send(session, Map.of("type", "error", "message", message));
    }

    private void send(WebSocketSession session, Object payload) throws IOException {
        if (session != null && session.isOpen()) {
            synchronized (session) {
                session.sendMessage(new TextMessage(jsonMapper.writeValueAsString(payload)));
            }
        }
    }
}
