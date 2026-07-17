package io.github.chiharusonh.territory;

import java.util.Arrays;

final class GameState {
    final int[] stones = new int[Board.VERTEX_COUNT];
    final int[] owners = new int[Board.FACES.length];
    final int[] cornersUsed = new int[3];
    int turn = 1;
    int ply;
    boolean over;
    int winner;
    String winReason = "";
    String status = "青の手番です。";

    static GameState onlineGame() {
        GameState state = new GameState();
        for (int face : Board.BLUE_HOME) {
            state.owners[face] = 1;
        }
        for (int face : Board.RED_HOME) {
            state.owners[face] = 2;
        }
        for (int vertex : Board.BLUE_STONES) {
            state.stones[vertex] = 1;
        }
        for (int vertex : Board.RED_STONES) {
            state.stones[vertex] = 2;
        }
        return state;
    }

    GameState copy() {
        GameState copy = new GameState();
        System.arraycopy(stones, 0, copy.stones, 0, stones.length);
        System.arraycopy(owners, 0, copy.owners, 0, owners.length);
        System.arraycopy(cornersUsed, 0, copy.cornersUsed, 0, cornersUsed.length);
        copy.turn = turn;
        copy.ply = ply;
        copy.over = over;
        copy.winner = winner;
        copy.winReason = winReason;
        copy.status = status;
        return copy;
    }

    @Override
    public String toString() {
        return "GameState{" +
                "stones=" + Arrays.toString(stones) +
                ", owners=" + Arrays.toString(owners) +
                ", turn=" + turn +
                ", ply=" + ply +
                ", over=" + over +
                '}';
    }
}
