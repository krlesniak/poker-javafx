package cards;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


class CardTest {

    @Test
    void testCompareTo() {
        Card ace1 = new Card(Rank.ACE, Suit.CLUBS);
        Card ace2 = new Card(Rank.ACE, Suit.HEARTS);
        Card king = new Card(Rank.KING, Suit.DIAMONDS);
        Card ten = new Card(Rank.TEN, Suit.SPADES);
        Card two = new Card(Rank.TWO, Suit.DIAMONDS);

        assertTrue(ace1.compareTo(king) > 0);
        assertTrue(king.compareTo(ace2) < 0);

        assertTrue(ten.compareTo(two) > 0);
        assertTrue(two.compareTo(ten) < 0);

        assertEquals(0, ace1.compareTo(ace2));
        assertEquals(0, ace2.compareTo(ace1));
    }

    @Test
    void testEquality() {
        Card card1 = new Card(Rank.ACE, Suit.SPADES);
        Card card2 = new Card(Rank.ACE, Suit.SPADES);
        Card card3 = new Card(Rank.ACE, Suit.HEARTS);

        Card card4 = new Card(Rank.TWO, Suit.SPADES);
        Card card5 = new Card(Rank.TEN, Suit.CLUBS);

        assertEquals(card1, card2); // the same rank and suit
        assertNotEquals(card1, card3); // the same rank but different suit

        assertNotEquals(card1, card4); // different rank the same suit
        assertNotEquals(card1, card5); // different rank different suit

    }

    @Test
    void testToString() {
        Card testCard = new Card(Rank.ACE, Suit.SPADES);
        assertEquals("A", testCard.rank().toString());
        assertEquals("♠", testCard.suit().toString());

        assertEquals("A ♠", testCard.toString());
    }

}