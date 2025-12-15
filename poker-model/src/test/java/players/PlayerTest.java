package players;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import exceptions.NotEnoughChipsException;

class PlayerTest {
    @Test
    void testBet(){
        Player testPlayer = new Player("p1", "John", 1000);

        assertEquals(1000, testPlayer.getChips());
        assertEquals("p1", testPlayer.getId());
        assertEquals("John", testPlayer.getName());

        testPlayer.bet(50);

        assertEquals(950, testPlayer.getChips());
        assertEquals(50, testPlayer.getCurrBet());
    }

    @Test
    void testBetError(){
        Player testPlayer = new Player("p1", "John", 50);
        assertThrows(NotEnoughChipsException.class, () -> {
            testPlayer.bet(100);
        });
    }

    @Test
    void testBetNegative(){
        Player testPlayer = new Player("p1", "John", -50);
        assertThrows(IllegalArgumentException.class, () -> {
            testPlayer.bet(-50);
        });
    }

    @Test
    void testBetZero(){
        Player testPlayer = new Player("p1", "John", 0);
        assertThrows(IllegalArgumentException.class, () -> {
            testPlayer.bet(0);
        });
    }

    @Test
    void testFold(){
        Player testPlayer = new Player("p1", "John", 1000);
        testPlayer.fold();
        assertTrue(testPlayer.isFolded());
    }

    @Test
    void testNotFold(){
        Player testPlayer = new Player("p1", "John", 1000);
        assertFalse(testPlayer.isFolded());
    }

    @Test
    void testResetRoundAndNewPhase(){
        Player testPlayer = new Player("p1", "John", 1000);

        testPlayer.bet(50);
        testPlayer.fold();

        // still player folded
        testPlayer.newPhaseReset();
        assertEquals(0, testPlayer.getCurrBet());
        assertTrue(testPlayer.isFolded());

        // round reset -> clears everything
        testPlayer.resetRound();
        assertEquals(0, testPlayer.getCurrBet());
        assertFalse(testPlayer.isFolded());
        assertEquals(0, testPlayer.getHand().size());
    }
}