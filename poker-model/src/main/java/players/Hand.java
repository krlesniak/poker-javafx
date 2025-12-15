package players;

import java.util.ArrayList;
import java.util.List;
import cards.Card;
import java.util.Collections;

/**
 * A class Hand that represents a player's hand that contains five cards
 */
public class Hand {
    private final List<Card> cards;

    /**
     * A constructor that creates a new list of cards
     */
    public Hand() {
        this.cards = new ArrayList<>();
    }

    /**
     * A method responsible for adding a cards to the player's hand
     * @param card from the deck
     */
    public void addCard(Card card) {
        cards.add(card);
    }

    /**
     * A method responsible for sorting cards in player's hand so it is easier to find layouts
     */
    // sorting cards in hand for easier comparing hand rankings
    public void sortHand(){
        Collections.sort(cards);
    }

    /**
     * A method responsible for returning indexes of cards that player wants to exchange from the deck
     * @param indexes of cards
     */
    public void removeCards(List<Integer> indexes){
        List<Card> cardsToRemove = new ArrayList<>();

        for (Integer i : indexes){
            cardsToRemove.add(cards.get(i));
        }

        cards.removeAll(cardsToRemove);
    }

    /**
     * getter
     * @return a new list of cards
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    /**
     * A method that returns an amount of cards in the hand
     * @return the size of cards in the hand
     */
    public int size() {
        return cards.size();
    }

    /**
     * A method that converts cards to string format
     * @return cards in string format
     */
    @Override
    public String toString() {
        return cards.toString();
    }
}
