package io.github.chiharusonh.territory;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
final class RoomService {
    record Participant(String roomCode, int player) {
    }

    record RoomView(String code, WebSocketSession host, WebSocketSession guest, GameState game) {
        boolean ready() {
            return host != null && guest != null;
        }
    }

    record MoveOutcome(RoomView room, GameEngine.MoveResult result) {
    }

    private static final int MAX_ROOM_ATTEMPTS = 100;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, MutableRoom> rooms = new HashMap<>();
    private final Map<String, Participant> participants = new HashMap<>();
    private final GameEngine engine = new GameEngine();

    synchronized RoomView create(WebSocketSession host) {
        leave(host);
        for (int attempt = 0; attempt < MAX_ROOM_ATTEMPTS; attempt++) {
            String code = "%06d".formatted(random.nextInt(1_000_000));
            if (!rooms.containsKey(code)) {
                MutableRoom room = new MutableRoom(code, host, GameState.onlineGame());
                rooms.put(code, room);
                participants.put(host.getId(), new Participant(code, 1));
                return room.view();
            }
        }
        throw new IllegalStateException("招待コードを発行できませんでした。");
    }

    synchronized Optional<RoomView> join(String code, WebSocketSession guest) {
        MutableRoom room = rooms.get(code);
        if (room == null || room.guest != null || !room.host.isOpen()) {
            return Optional.empty();
        }
        leave(guest);
        room.guest = guest;
        participants.put(guest.getId(), new Participant(code, 2));
        return Optional.of(room.view());
    }

    synchronized Optional<MoveOutcome> move(WebSocketSession session, int vertex) {
        Participant participant = participants.get(session.getId());
        if (participant == null) {
            return Optional.empty();
        }
        MutableRoom room = rooms.get(participant.roomCode());
        if (room == null || !room.ready()) {
            return Optional.empty();
        }
        GameEngine.MoveResult result = engine.place(room.game, participant.player(), vertex);
        return Optional.of(new MoveOutcome(room.view(), result));
    }

    synchronized Optional<RoomView> roomFor(WebSocketSession session) {
        Participant participant = participants.get(session.getId());
        if (participant == null) {
            return Optional.empty();
        }
        MutableRoom room = rooms.get(participant.roomCode());
        return room == null ? Optional.empty() : Optional.of(room.view());
    }

    synchronized Optional<RoomView> leave(WebSocketSession session) {
        Participant participant = participants.remove(session.getId());
        if (participant == null) {
            return Optional.empty();
        }
        MutableRoom room = rooms.remove(participant.roomCode());
        if (room == null) {
            return Optional.empty();
        }
        if (room.host != null) {
            participants.remove(room.host.getId());
        }
        if (room.guest != null) {
            participants.remove(room.guest.getId());
        }
        return Optional.of(room.view());
    }

    private static final class MutableRoom {
        private final String code;
        private final WebSocketSession host;
        private final GameState game;
        private WebSocketSession guest;

        private MutableRoom(String code, WebSocketSession host, GameState game) {
            this.code = code;
            this.host = host;
            this.game = game;
        }

        private boolean ready() {
            return host != null && guest != null;
        }

        private RoomView view() {
            return new RoomView(code, host, guest, game.copy());
        }
    }
}
