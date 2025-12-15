package cards;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class RankSuitCardTest {
    @Test
    void testRankValues() {
        assertEquals(2, Rank.TWO.getValue());
        assertEquals(3, Rank.THREE.getValue());
        assertEquals(4, Rank.FOUR.getValue());
        assertEquals(5, Rank.FIVE.getValue());
        assertEquals(6, Rank.SIX.getValue());
        assertEquals(7, Rank.SEVEN.getValue());
        assertEquals(8, Rank.EIGHT.getValue());
        assertEquals(9, Rank.NINE.getValue());
        assertEquals(10, Rank.TEN.getValue());
        assertEquals(11, Rank.JACK.getValue());
        assertEquals(12, Rank.QUEEN.getValue());
        assertEquals(13, Rank.KING.getValue());
        assertEquals(14, Rank.ACE.getValue());
    }

    @Test
    void restRankValuesToString(){
        assertEquals("2", Rank.TWO.toString());
        assertEquals("3", Rank.THREE.toString());
        assertEquals("4", Rank.FOUR.toString());
        assertEquals("5", Rank.FIVE.toString());
        assertEquals("6", Rank.SIX.toString());
        assertEquals("7", Rank.SEVEN.toString());
        assertEquals("8", Rank.EIGHT.toString());
        assertEquals("9", Rank.NINE.toString());
        assertEquals("10", Rank.TEN.toString());
        assertEquals("J", Rank.JACK.toString());
        assertEquals("Q", Rank.QUEEN.toString());
        assertEquals("K", Rank.KING.toString());
        assertEquals("A", Rank.ACE.toString());
    }

    @Test
    void testRankOrder() {
        assertTrue(Rank.ACE.getValue() > Rank.JACK.getValue());
        assertFalse(Rank.ACE.getValue() < Rank.QUEEN.getValue());
    }

    @Test
    void testSuitValues() {
        assertEquals("♠", Suit.SPADES.toString());
        assertEquals("♣", Suit.CLUBS.toString());
        assertEquals("♥", Suit.HEARTS.toString());
        assertEquals("♦", Suit.DIAMONDS.toString());
    }


}