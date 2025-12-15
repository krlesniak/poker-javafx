package hierarchy;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cards.Suit;
import cards.Rank;
import cards.Card;
import players.Hand;


class HandCheckerTest {

    private HandChecker testHandChecker;

    @BeforeEach
    void setUp() {
        testHandChecker = new HandChecker();
    }

    // creating a random hand
    private Hand createTestHand (Rank r1, Suit s1, Rank r2, Suit s2, Rank r3, Suit s3, Rank r4, Suit s4, Rank r5, Suit s5) {
        Hand testHand = new Hand();

        testHand.addCard(new Card(r1, s1));
        testHand.addCard(new Card(r2, s2));
        testHand.addCard(new Card(r3, s3));
        testHand.addCard(new Card(r4, s4));
        testHand.addCard(new Card(r5, s5));
        return testHand;
    }

    @Test
    void testPair(){
        Hand testHand = createTestHand(
                Rank.JACK, Suit.SPADES,
                Rank.KING, Suit.CLUBS,
                Rank.JACK, Suit.HEARTS,
                Rank.TEN, Suit.DIAMONDS,
                Rank.TWO, Suit.SPADES
                );

        HandValue res = testHandChecker.checker(testHand);
        assertEquals(HandRanking.PAIR, res.ranking());
    }

    @Test
    void testTwoPairs() {
        Hand testHand = createTestHand(
                Rank.JACK, Suit.SPADES,
                Rank.KING, Suit.CLUBS,
                Rank.JACK, Suit.HEARTS,
                Rank.TEN, Suit.DIAMONDS,
                Rank.KING, Suit.SPADES
        );

        HandValue res = testHandChecker.checker(testHand);
        assertEquals(HandRanking.TWO_PAIRS, res.ranking());
    }

    @Test
    void testThreeOfAKind() {
        Hand testHand = createTestHand(
                Rank.JACK, Suit.SPADES,
                Rank.TEN, Suit.CLUBS,
                Rank.TEN, Suit.HEARTS,
                Rank.TEN, Suit.DIAMONDS,
                Rank.KING, Suit.SPADES
        );

        HandValue res = testHandChecker.checker(testHand);
        assertEquals(HandRanking.THREE_OAK, res.ranking());
    }

    @Test
    void testFourOfAKind() {
        Hand testHand = createTestHand(
                Rank.TEN, Suit.SPADES,
                Rank.TEN, Suit.CLUBS,
                Rank.TEN, Suit.HEARTS,
                Rank.FOUR, Suit.DIAMONDS,
                Rank.TEN, Suit.SPADES
        );

        HandValue res = testHandChecker.checker(testHand);
        assertEquals(HandRanking.FOUR_OAK, res.ranking());
    }

    @Test
    void testFullHouse() {
        Hand testHand = createTestHand(
                Rank.KING, Suit.SPADES,
                Rank.TEN, Suit.CLUBS,
                Rank.TEN, Suit.HEARTS,
                Rank.TEN, Suit.DIAMONDS,
                Rank.KING, Suit.SPADES
        );

        HandValue res = testHandChecker.checker(testHand);
        assertEquals(HandRanking.FULL_HOUSE, res.ranking());
    }

    @Test
    void testFlush() {
        Hand testHand = createTestHand(
                Rank.JACK, Suit.SPADES,
                Rank.TEN, Suit.SPADES,
                Rank.NINE, Suit.SPADES,
                Rank.FIVE, Suit.SPADES,
                Rank.KING, Suit.SPADES
        );

        HandValue res = testHandChecker.checker(testHand);
        assertEquals(HandRanking.FLUSH, res.ranking());
    }

    @Test
    void testStraight() {
        Hand testHand = createTestHand(
                Rank.SEVEN, Suit.SPADES,
                Rank.TEN, Suit.CLUBS,
                Rank.NINE, Suit.HEARTS,
                Rank.EIGHT, Suit.DIAMONDS,
                Rank.SIX, Suit.SPADES
        );

        HandValue res = testHandChecker.checker(testHand);
        assertEquals(HandRanking.STRAIGHT, res.ranking());
    }

    @Test
    void testRoyalFlush() {
        Hand testHand = createTestHand(
                Rank.ACE, Suit.SPADES,
                Rank.QUEEN, Suit.SPADES,
                Rank.TEN, Suit.SPADES,
                Rank.JACK, Suit.SPADES,
                Rank.KING, Suit.SPADES
        );

        HandValue res = testHandChecker.checker(testHand);
        assertEquals(HandRanking.ROYAL_FLUSH, res.ranking());
    }

    @Test
    void testStraightFlush() {
        Hand testHand = createTestHand(
                Rank.ACE, Suit.DIAMONDS,
                Rank.QUEEN, Suit.DIAMONDS,
                Rank.TEN, Suit.DIAMONDS,
                Rank.JACK, Suit.DIAMONDS,
                Rank.KING, Suit.DIAMONDS
        );

        HandValue res = testHandChecker.checker(testHand);
        assertEquals(HandRanking.ROYAL_FLUSH, res.ranking());
    }

    @Test
    void testCompareLayouts() {
        // Royal Flush
        Hand testRF = createTestHand(
                Rank.ACE, Suit.DIAMONDS,
                Rank.QUEEN, Suit.DIAMONDS,
                Rank.TEN, Suit.DIAMONDS,
                Rank.JACK, Suit.DIAMONDS,
                Rank.KING, Suit.DIAMONDS
        );

        // Straight
        Hand testStr = createTestHand(
                Rank.SEVEN, Suit.SPADES,
                Rank.TEN, Suit.CLUBS,
                Rank.NINE, Suit.HEARTS,
                Rank.EIGHT, Suit.DIAMONDS,
                Rank.SIX, Suit.SPADES
        );

        // Full House
        Hand testFH = createTestHand(
                Rank.KING, Suit.SPADES,
                Rank.TEN, Suit.CLUBS,
                Rank.TEN, Suit.HEARTS,
                Rank.TEN, Suit.DIAMONDS,
                Rank.KING, Suit.SPADES
        );

        // Two Pairs 1
        Hand testTwoP1 = createTestHand(
                Rank.JACK, Suit.SPADES,
                Rank.KING, Suit.CLUBS,
                Rank.JACK, Suit.HEARTS,
                Rank.TEN, Suit.DIAMONDS,
                Rank.KING, Suit.SPADES
        );

        // Two Pairs 2
        Hand testTwoP2 = createTestHand(
                Rank.ACE, Suit.SPADES,
                Rank.KING, Suit.CLUBS,
                Rank.ACE, Suit.HEARTS,
                Rank.TEN, Suit.DIAMONDS,
                Rank.KING, Suit.SPADES
        );

        // results
        HandValue resRF = testHandChecker.checker(testRF);
        HandValue resStr = testHandChecker.checker(testStr);
        HandValue resFH = testHandChecker.checker(testFH);
        HandValue resTwoP1 = testHandChecker.checker(testTwoP1);
        HandValue resTwoP2 = testHandChecker.checker(testTwoP2);

        assertTrue(resRF.compareTo(resFH) > 0); // 1
        assertTrue(resFH.compareTo(resStr) > 0); // 1
        assertTrue(resStr.compareTo(resTwoP1) > 0); // 1

        assertTrue(resTwoP2.compareTo(resTwoP1) > 0); // 1 because Ace > King
    }
}