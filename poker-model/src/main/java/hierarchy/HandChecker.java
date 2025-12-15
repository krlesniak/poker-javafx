package hierarchy;

import java.util.*;

import players.Hand;
import cards.Card;
import cards.Rank;
import cards.Suit;


/**
 * A class that checks if hand of five cards has any possible layouts checking by the highest to the lowest
 */
public class HandChecker {

    /**
     * Constructs the HandChecker instance.
     * This instance is typically used to calculate the value of poker hands.
     */
    public HandChecker() {
        // empty
    }
    /**
     * A method that checks the power of layout in the hand
     * @param hand of the player that contains of five cards
     * @return the highest layout in the hand and a list of deciders when there is a tie
     */
    public HandValue checker(Hand hand) {
        if (hand.size() != 5) {
            throw new IllegalArgumentException("We need 5 cards in hand ONLY");
        }
        List<Card> cards = new ArrayList<>(hand.getCards());
        cards.sort(Collections.reverseOrder()); // from the highest to the lowest

        boolean flush = checkFlush(cards);
        boolean straight = checkStraight(cards);

        // counting appearances of every card
        Map<Rank, Integer> counts = new HashMap<>();
        for (Card c : cards) {
            Rank r = c.rank();
            if (counts.containsKey(r)) {
                counts.put(r, counts.get(r) + 1);
            } else {
                counts.put(r, 1);
            }
        }

        List<Rank> sortedRanks = new ArrayList<>(counts.keySet());

        // creating Comparator object
        Comparator<Rank> rankSorter = new RankComparator(counts);

        // sorting
        sortedRanks.sort(rankSorter);

        List<Integer> decider = new ArrayList<>();
        for (Rank r : sortedRanks) {
            decider.add(r.getValue());
        }

        HandValue layout;

        // royal flush / straigh flush
        layout = checkForStraightFlush(cards, flush, straight);
        if (layout != null) {return layout;}

        // four of a kind
        layout = checkForFourOfAKind(counts, decider);
        if (layout != null) {return layout;}

        // full house
        layout = checkForFullHouse(counts, decider);
        if (layout != null) {return layout;}

        // flush
        if (flush) {
            List<Integer> flushDeciders= new ArrayList<>();
            for (Card c : cards) {
                flushDeciders.add(c.rank().getValue());
            }
            return new HandValue(HandRanking.FLUSH, flushDeciders);
        }

        // straight
        if (straight) {
            List<Integer> straightBreakers = new ArrayList<>();
            straightBreakers.add(cards.get(0).rank().getValue());
            return new HandValue(HandRanking.STRAIGHT, straightBreakers);
        }

        // three of a kind
        layout = checkForThreeOfAKind(counts, decider);
        if (layout != null) {return layout;}

        // two pairs
        layout = checkForTwoPairs(counts, decider);
        if (layout != null) {return layout;}

        // pair
        layout = checkForPair(counts, decider);
        if (layout != null) {return layout;}

        // high card if not any of other layouts
        return new HandValue(HandRanking.HIGH_CARD, decider);
    }

    // |-----------------------------------------|
    // |=--=-=-=-=- ALL HAND LAYOUTS -=--=-=-=-=-|
    // |-----------------------------------------|


    /**
     * A method that checks if there is a layout of
     * one pair of cards of the same rank
     * @param counts a map that counts appearances of every card in the hand
     * @param decider a list of values of the cards needed when there is a tie and
     *                ranks of cards are crucial
     * @return new layout pair if there is one
     */
    private HandValue checkForPair(Map<Rank, Integer> counts, List<Integer> decider) {
        if (counts.containsValue(2)) {
            return new HandValue(HandRanking.PAIR, decider);
        }
        return null;
    }

    /**
     * A method that checks if there is a layout of
     * two pairs of cards of the same rank
     * @param counts a map that counts appearances of every card in the hand
     * @param decider a list of values of the cards needed when there is a tie and
     *                ranks of cards are crucial
     * @return new layout two pairs if there is one
     */
    private HandValue checkForTwoPairs(Map<Rank, Integer> counts, List<Integer> decider) {
        int pairCount = 0;
        for (int c : counts.values()) {
            if (c == 2) pairCount++;
        }
        if (pairCount == 2) {
            return new HandValue(HandRanking.TWO_PAIRS, decider);
        }
        return null;
    }

    /**
     * A method that checks if there is a layout of
     * three cards of the same rank
     * @param counts a map that counts appearances of every card in the hand
     * @param decider a list of values of the cards needed when there is a tie and
     *                ranks of cards are crucial
     * @return new layout three of a kind if there is one
     */
    // three of a kind
    private HandValue checkForThreeOfAKind(Map<Rank, Integer> counts, List<Integer> decider) {
        if (counts.containsValue(3)) {
            return new HandValue(HandRanking.THREE_OAK, decider);
        }
        return null;
    }

    /**
     * A method that checks if there is a layout of
     * four cards of the same rank
     * @param counts a map that counts appearances of every card in the hand
     * @param decider a list of values of the cards needed when there is a tie and
     *                ranks of cards are crucial
     * @return new layout four of a kind if there is one
     */
    // four of a kind
    private HandValue checkForFourOfAKind(Map<Rank, Integer> counts, List<Integer> decider) {
        if (counts.containsValue(4)) {
            return new HandValue(HandRanking.FOUR_OAK, decider);
        }
        return null;
    }

    /**
     * A method that checks if there is a layout of
     * one pair of cards of the same rank and three other cards of the same rank
     * @param counts a map that counts appearances of every card in the hand
     * @param decider a list of values of the cards needed when there is a tie and
     *                ranks of cards are crucial
     * @return new layout full house if there is one
     */
    // full house
    private HandValue checkForFullHouse(Map<Rank, Integer> counts, List<Integer> decider) {
        if (counts.containsValue(2) && counts.containsValue(3)) {
            return new HandValue(HandRanking.FULL_HOUSE, decider);
        }
        return null;
    }

    /**
     * A method that checks if there is a layout of
     * five cards every card is on value lower than the previous one
     * but starting from ACE (Royal Flush)
     * or no (Straight Flush)
     * @param cards list of cards in the hand
     * @param flush a boolean variable that says if there is a flush
     * @param straight a boolean variable that says if there is a straight
     * @return new layout Royal Flush or Straight Flush if there is one
     */
    // Royal Flush and straight flush
    private HandValue checkForStraightFlush(List<Card> cards, boolean flush, boolean straight) {
        if (straight && flush) {
            if (cards.get(0).rank() == Rank.ACE) {
                return new HandValue(HandRanking.ROYAL_FLUSH, new ArrayList<>());
            }
            // first card decides
            List<Integer> straightDecider = new ArrayList<>();
            straightDecider.add(cards.get(0).rank().getValue());
            return new HandValue(HandRanking.STRAIGHT_FLUSH, straightDecider);
        }
        return null;
    }

    /**
     * A method that checks if there is a flush in the hand
     * @param cards in the hand
     * @return true if there is a flush or false if there is not
     */
    private boolean checkFlush(List<Card> cards) {
        // color of the first card
        Suit suit = cards.get(0).suit();
        // checking if every other card is the same color
        for (Card c : cards) {
            if (c.suit() != suit) {
                return false;
            }
        }
        return true;
    }

    /**
     * A method that checks if there is a straight in the hand
     * @param cards in the hand
     * @return true if there is a straight or false if there is not
     */
    private boolean checkStraight(List<Card> cards) {
        // cards are sorted desc
        for (int i = 0; i < cards.size() - 1; i++) {
            int current = cards.get(i).rank().getValue();
            int next = cards.get(i + 1).rank().getValue();
            if (current - next != 1) {
                return false;
            }
        }
        return true;
    }
}
