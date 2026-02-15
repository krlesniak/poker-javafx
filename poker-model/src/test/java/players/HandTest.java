package players;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import cards.Card;
import cards.Rank;
import cards.Suit;
import cards.Deck;
import java.util.ArrayList;
import java.util.List;

class HandTest {

    @Test
    void testAddCards(){
        Hand testHand = new Hand();

        testHand.addCard(new Card(Rank.QUEEN, Suit.SPADES));
        testHand.addCard(new Card(Rank.JACK, Suit.SPADES));
        testHand.addCard(new Card(Rank.KING, Suit.SPADES));

        assertEquals(3, testHand.size());
    }

    @Test
    void testSortHand(){
        Hand testHand = new Hand();

        testHand.addCard(new Card(Rank.QUEEN, Suit.SPADES));
        testHand.addCard(new Card(Rank.JACK, Suit.SPADES));
        testHand.addCard(new Card(Rank.KING, Suit.SPADES));
        testHand.addCard(new Card(Rank.NINE, Suit.HEARTS));
        testHand.addCard(new Card(Rank.KING, Suit.HEARTS));

        testHand.sortHand();

        assertEquals(5, testHand.size());

        assertEquals(Rank.NINE, testHand.getCards().get(0).rank());
        assertEquals(Rank.JACK, testHand.getCards().get(1).rank());
        assertEquals(Rank.QUEEN, testHand.getCards().get(2).rank());
        assertEquals(Rank.KING, testHand.getCards().get(3).rank());

        assertEquals(Suit.SPADES, testHand.getCards().get(3).suit());
        assertEquals(Suit.HEARTS, testHand.getCards().get(4).suit());

    }
}