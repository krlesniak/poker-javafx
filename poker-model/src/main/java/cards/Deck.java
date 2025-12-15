package cards;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


/**
 * A class that represents a whole deck of cards, it contains a total of 52 cards, 4 suits and 13 ranks
 */
public class Deck {
    /**
     * A list that contains cards
     */
    private final List<Card> cards;
    /**
     * an SecureRandom object that helps us while shuffling the deck
     */
    private final SecureRandom random;

    /**
     * A public constructor Deck
     */
    public Deck() {
        this.cards = new ArrayList<>();
        this.random = new SecureRandom();
        reset();
    }

    /**
     * A method that resets teh deck, clears a list of cards,
     * fills the list with cards in standard order and then
     * shuffles the deck so the standing of cards is different
     */
    public void reset() {
        cards.clear();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        shuffle();
    }

    /**
     * A method that is responsible for shuffling the deck of cards
     */
    public void shuffle() {
        Collections.shuffle(cards, random);
    }

    /**
     * A method that is responsible for dealing cards and removing them from the deck
     * it is optional in case there is last card in deck then it is null
     * the same goes with return object
     * @return card if it is available (optional when there is no card)
     */
    public Optional<Card> deal() {
        if (cards.isEmpty()) return Optional.empty();
        return Optional.of(cards.remove(cards.size() - 1));
    }

    /**
     * A method that returns how many cards is in teh deck
     * @return the size of the current deck
     */
    public int size() {
        return cards.size();
    }
}