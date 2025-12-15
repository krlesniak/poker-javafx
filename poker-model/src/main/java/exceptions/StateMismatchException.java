package exceptions;

import game.GameState;

/**
 * Exception thrown when the state of round (BET1, DRAW, BET2, ...) is not correct according to the rules of the game
 */
public final class StateMismatchException extends InvalidMoveException {
    /**
     * A constructor that makes a StateMismatchException
     * @param actualState a state that called out an exception (invalid state)
     * @param expectedState a state that is correct according to the rules of poker game
     */
    public StateMismatchException(GameState actualState, GameState expectedState) {
        super("STATE_MISMATCH", "Invalid state of the game: " + actualState + ", Expected: " + expectedState);
    }
}
