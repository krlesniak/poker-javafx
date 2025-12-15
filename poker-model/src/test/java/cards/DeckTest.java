package cards;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

class DeckTest {
    @Test
    void testSize(){
        Deck testDeck = new Deck();
        assertEquals(testDeck.size(),52);
    }

    @Test
    void testSizeAfterDeal(){
        Deck testDeck = new Deck();
        int s =  testDeck.size();

        testDeck.deal();
        assertEquals(testDeck.size(),s-1);
    }

    @Test
    void testReset(){
        Deck testDeck = new Deck();

        testDeck.deal();
        testDeck.deal();
        testDeck.deal();

        testDeck.reset();
        assertEquals(testDeck.size(),52);
    }

    @Test
    void testDeckUniqueCards() {
        Deck deck = new Deck();
        Set<Card> takenCards = new HashSet<>();

        for (int i = 0; i < 52; i++) {
            Optional<Card> cardOpt = deck.deal();
            assertTrue(cardOpt.isPresent());
            takenCards.add(cardOpt.get());
        }

        assertEquals(52, takenCards.size());
        assertEquals(0, deck.size());
    }

    @Test
    void testDealEmptyDeck() {
        Deck testDeck = new Deck();
        for (int i = 0; i < 52; i++) {
            testDeck.deal();
        }

        Optional<Card> emptyCard = testDeck.deal();
        assertTrue(emptyCard.isEmpty());
    }

}