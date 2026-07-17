package io.github.chiharusonh.territory;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameEngineTest {
    private final GameEngine engine = new GameEngine();

    @Test
    void onlineGameStartsWithOneTerritoryEachAndBlueTurn() {
        GameState state = GameState.onlineGame();

        assertThat(engine.countOf(state, 1)).isEqualTo(1);
        assertThat(engine.countOf(state, 2)).isEqualTo(1);
        assertThat(state.turn).isEqualTo(1);
    }

    @Test
    void firstMoveCannotUseCorner() {
        GameState state = GameState.onlineGame();

        assertThat(engine.canPlace(state, 0, 1)).isFalse();
        assertThat(engine.place(state, 1, 0).accepted()).isFalse();
    }

    @Test
    void playerCannotMoveOutOfTurn() {
        GameState state = GameState.onlineGame();

        GameEngine.MoveResult result = engine.place(state, 2, 10);

        assertThat(result.accepted()).isFalse();
        assertThat(state.stones[10]).isZero();
    }

    @Test
    void deadEndWallsArePruned() {
        GameState state = new GameState();
        state.stones[23] = 2;
        state.stones[24] = 2;
        state.stones[31] = 2;

        assertThat(engine.prunedWallEdges(state, 2)).isEmpty();
        assertThat(engine.enclosedBy(state, 2)).isEmpty();
    }

    @Test
    void fourCornersCaptureAClosedSquare() {
        GameState state = new GameState();
        state.ply = 1;
        state.turn = 1;
        state.stones[2] = 1;
        state.stones[3] = 1;
        state.stones[8] = 1;

        GameEngine.MoveResult result = engine.place(state, 1, 9);

        assertThat(result.accepted()).isTrue();
        assertThat(state.owners[2]).isEqualTo(1);
        assertThat(engine.countOf(state, 1)).isEqualTo(1);
    }

    @Test
    void enclosedCalculationIsDeterministic() {
        GameState state = new GameState();
        state.stones[2] = 1;
        state.stones[3] = 1;
        state.stones[8] = 1;
        state.stones[9] = 1;

        Set<Integer> first = engine.enclosedBy(state, 1);
        Set<Integer> second = engine.enclosedBy(state, 1);

        assertThat(second).isEqualTo(first).contains(2);
    }
}
