package exceptions;

import static org.junit.jupiter.api.Assertions.*;
import game.GameState;
import cards.Deck;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

    @Test
    void testIllegalDrawException() {
        IllegalDrawException ex = new IllegalDrawException("Too many cards");
        assertEquals("ILLEGAL_DRAW", ex.getCode());
        assertEquals("Too many cards", ex.getMessage());
    }

    @Test
    void testInvalidMoveExceptionBase() {
        InvalidMoveException ex = new InvalidMoveException("TEST_CODE", "Test message");

        assertEquals("TEST_CODE", ex.getCode());
        assertEquals("Test message", ex.getMessage());
    }

    @Test
    void testNotEnoughChipsException() {
        NotEnoughChipsException ex = new NotEnoughChipsException(100, 50);

        assertEquals("NO_CHIPS", ex.getCode());

        assertTrue(ex.getMessage().contains("100"));
        assertTrue(ex.getMessage().contains("50"));
    }

    @Test
    void testOutOfTurnException() {
        OutOfTurnException ex = new OutOfTurnException();

        assertEquals("OUT_OF_TURN", ex.getCode());
        assertTrue(ex.getMessage().contains("It is not your turn"));
    }

    @Test
    void testProtocolException() {
        ProtocolException ex = new ProtocolException("BAD_CMD", "Unknown command");
        assertEquals("PROTOCOL_ERROR", ex.getCode());
        assertTrue(ex.getMessage().contains("BAD_CMD"));
    }

    @Test
    void testSecurityException() {
        SecurityException ex = new SecurityException("Illegal card swap attempt");

        assertEquals("SECURITY_VIOLATION", ex.getCode());
        assertTrue(ex.getMessage().contains("Illegal card swap attempt"));
    }

    @Test
    void testStateMismatchException() {
        StateMismatchException ex = new StateMismatchException(GameState.DRAW, GameState.BET1);

        assertEquals("STATE_MISMATCH", ex.getCode());
        assertTrue(ex.getMessage().contains("BET1"));
    }

}