package io.github.chiharusonh.territory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.chiharusonh.territory.Board.Edge;

final class GameEngine {
    record MoveResult(boolean accepted, String message) {
        static MoveResult accepted(String message) {
            return new MoveResult(true, message);
        }

        static MoveResult rejected(String message) {
            return new MoveResult(false, message);
        }
    }

    MoveResult place(GameState state, int player, int vertex) {
        if (state.over) {
            return MoveResult.rejected("この対局は終了しています。");
        }
        if (player != state.turn) {
            return MoveResult.rejected("相手の手番です。");
        }
        String illegalReason = illegalReason(state, vertex, player);
        if (illegalReason != null) {
            return MoveResult.rejected(illegalReason);
        }

        int beforeScore = countOf(state, player);
        Set<Integer> beforeEnclosed = enclosedBy(state, player);
        state.stones[vertex] = player;
        if (Board.CORNERS.contains(vertex)) {
            state.cornersUsed[player]++;
        }
        state.ply++;

        captureCompletedFaces(state, player, beforeEnclosed);
        cleanupInteriorStones(state);

        int winner = homeWinner(state);
        if (winner != 0) {
            state.over = true;
            state.winner = winner;
            state.winReason = "相手の初期領地をすべて奪いました！";
            state.status = colorName(winner) + "の勝利です。";
            return MoveResult.accepted(state.status);
        }

        int gained = countOf(state, player) - beforeScore;
        String base = gained > 0
                ? colorName(player) + "が" + gained + "面を領地にしました。"
                : colorName(player) + "が石を置きました。";
        String turnMessage = advanceTurn(state);
        if (state.over) {
            return MoveResult.accepted(state.status);
        }
        state.status = turnMessage.isEmpty() ? base : base + " " + turnMessage;
        return MoveResult.accepted(state.status);
    }

    boolean canPlace(GameState state, int vertex, int player) {
        return illegalReason(state, vertex, player) == null;
    }

    List<Integer> legalMoves(GameState state, int player) {
        List<Integer> moves = new ArrayList<>();
        for (int vertex = 0; vertex < Board.VERTEX_COUNT; vertex++) {
            if (canPlace(state, vertex, player)) {
                moves.add(vertex);
            }
        }
        return moves;
    }

    int countOf(GameState state, int player) {
        int count = 0;
        for (int owner : state.owners) {
            if (owner == player) {
                count++;
            }
        }
        return count;
    }

    Set<Edge> wallEdges(GameState state, int player) {
        Set<Edge> wall = new HashSet<>();
        for (int[] rail : Board.RAILS) {
            int first = -1;
            int last = -1;
            for (int i = 0; i < rail.length; i++) {
                if (state.stones[rail[i]] == player) {
                    if (first < 0) {
                        first = i;
                    }
                    last = i;
                }
            }
            if (first >= 0 && last > first) {
                for (int i = first; i < last; i++) {
                    wall.add(new Edge(rail[i], rail[i + 1]));
                }
            }
        }
        return wall;
    }

    Set<Edge> prunedWallEdges(GameState state, int player) {
        Set<Edge> wall = new HashSet<>(wallEdges(state, player));
        Map<Integer, List<Edge>> incident = new HashMap<>();
        for (Edge edge : wall) {
            incident.computeIfAbsent(edge.a(), ignored -> new ArrayList<>()).add(edge);
            incident.computeIfAbsent(edge.b(), ignored -> new ArrayList<>()).add(edge);
        }

        boolean changed;
        do {
            changed = false;
            for (List<Edge> edges : incident.values()) {
                Edge only = null;
                int count = 0;
                for (Edge edge : edges) {
                    if (wall.contains(edge)) {
                        only = edge;
                        count++;
                    }
                }
                if (count == 1 && wall.remove(only)) {
                    changed = true;
                }
            }
        } while (changed);
        return wall;
    }

    Set<Integer> enclosedBy(GameState state, int player) {
        Set<Edge> wall = prunedWallEdges(state, player);
        boolean[] seen = new boolean[Board.FACES.length];
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        for (Edge boundary : Board.BOUNDARY_EDGES) {
            if (!wall.contains(boundary)) {
                int face = Board.EDGE_FACES.get(boundary).getFirst();
                if (!seen[face]) {
                    seen[face] = true;
                    queue.add(face);
                }
            }
        }

        while (!queue.isEmpty()) {
            int face = queue.removeFirst();
            for (Edge edge : Board.FACE_EDGES.get(face)) {
                if (wall.contains(edge)) {
                    continue;
                }
                for (int neighbor : Board.EDGE_FACES.get(edge)) {
                    if (!seen[neighbor]) {
                        seen[neighbor] = true;
                        queue.add(neighbor);
                    }
                }
            }
        }

        Set<Integer> enclosed = new HashSet<>();
        for (int face = 0; face < seen.length; face++) {
            if (!seen[face]) {
                enclosed.add(face);
            }
        }
        return enclosed;
    }

    private String illegalReason(GameState state, int vertex, int player) {
        if (vertex < 0 || vertex >= Board.VERTEX_COUNT) {
            return "存在しない交点です。";
        }
        if (state.over) {
            return "この対局は終了しています。";
        }
        if (state.stones[vertex] != 0) {
            return "そこには石があります。";
        }
        if (strictInteriorOwner(state, vertex) != 0) {
            return "領地の内部には置けません。";
        }
        if (state.ply == 0 && Board.CORNERS.contains(vertex)) {
            return "先手の初手は角に置けません。";
        }
        if (Board.CORNERS.contains(vertex) && state.cornersUsed[player] >= 2) {
            return "角は2つまでです。";
        }
        return null;
    }

    private int strictInteriorOwner(GameState state, int vertex) {
        List<Integer> faces = Board.INCIDENT_FACES.get(vertex);
        if (faces.isEmpty() || Board.BOUNDARY_VERTICES.contains(vertex)) {
            return 0;
        }
        int owner = state.owners[faces.getFirst()];
        if (owner == 0) {
            return 0;
        }
        for (int face : faces) {
            if (state.owners[face] != owner) {
                return 0;
            }
        }
        return owner;
    }

    private void captureCompletedFaces(GameState state, int player, Set<Integer> beforeEnclosed) {
        Set<Integer> afterEnclosed = enclosedBy(state, player);
        boolean hasNewFace = afterEnclosed.stream().anyMatch(face -> !beforeEnclosed.contains(face));
        if (!hasNewFace) {
            return;
        }
        for (int face : afterEnclosed) {
            state.owners[face] = player;
        }
    }

    private void cleanupInteriorStones(GameState state) {
        List<Integer> removed = new ArrayList<>();
        for (int vertex = 0; vertex < Board.VERTEX_COUNT; vertex++) {
            if (state.stones[vertex] != 0 && strictInteriorOwner(state, vertex) != 0) {
                removed.add(vertex);
            }
        }
        for (int vertex : removed) {
            state.stones[vertex] = 0;
        }
        if (!removed.isEmpty()) {
            recountCorners(state);
        }
    }

    private void recountCorners(GameState state) {
        Arrays.fill(state.cornersUsed, 0);
        for (int corner : Board.CORNERS) {
            int player = state.stones[corner];
            if (player != 0) {
                state.cornersUsed[player]++;
            }
        }
    }

    private int homeWinner(GameState state) {
        if (allOwnedBy(state, Board.RED_HOME, 1)) {
            return 1;
        }
        if (allOwnedBy(state, Board.BLUE_HOME, 2)) {
            return 2;
        }
        return 0;
    }

    private boolean allOwnedBy(GameState state, int[] faces, int player) {
        for (int face : faces) {
            if (state.owners[face] != player) {
                return false;
            }
        }
        return true;
    }

    private String advanceTurn(GameState state) {
        int current = state.turn;
        int next = 3 - current;
        if (!legalMoves(state, next).isEmpty()) {
            state.turn = next;
            return "";
        }
        if (!legalMoves(state, current).isEmpty()) {
            return colorName(next) + "は置ける点がないため、自動で手番を渡しました。";
        }
        endByScore(state);
        return "";
    }

    private void endByScore(GameState state) {
        int blue = countOf(state, 1);
        int red = countOf(state, 2);
        state.over = true;
        if (blue > red) {
            state.winner = 1;
        } else if (red > blue) {
            state.winner = 2;
        } else {
            state.winner = 0;
        }
        state.winReason = state.winner == 0
                ? "引き分け（" + red + " 対 " + blue + "）"
                : "図形数 " + Math.max(blue, red) + " 対 " + Math.min(blue, red) + " で勝利";
        state.status = state.winner == 0 ? "引き分けです。" : colorName(state.winner) + "の勝利です。";
    }

    private String colorName(int player) {
        return player == 1 ? "青" : "赤";
    }
}
