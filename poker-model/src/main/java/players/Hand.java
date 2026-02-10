package players;

import java.util.ArrayList;
import java.util.List;
import cards.Card;
import java.util.Collections;

public class Hand {
    private final List<Card> cards;

    public Hand() {
        this.cards = new ArrayList<>();
    }

    public void addCard(Card card) {
        cards.add(card);
    }

    public void clear() {
        cards.clear();
    }

    public void sortHand(){
        Collections.sort(cards);
    }

    // method for replacing cards on chosen positions
    public void replace(int index, Card card) {
        if (index >= 0 && index < cards.size()) {
            cards.set(index, card);
        }
    }

    public void removeCards(List<Integer> indexes){
        indexes.sort(Collections.reverseOrder());
        for (int i : indexes) {
            if (i >= 0 && i < cards.size()) {
                cards.remove(i);
            }
        }
    }

    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    public int size() {
        return cards.size();
    }

    @Override
    public String toString() {
        return cards.toString();
    }
}