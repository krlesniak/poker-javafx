package cards;

import java.util.Objects;

/**
 * a record class that represents one card with rank and suit
 * it implements an interface Comparable and because of that we cn compare cards based on their value
 * @param rank of card, for example ACE, KING, TEN, FIVE ...
 * @param suit of card, for example SPADES, DIAMONDS, HEARTS, CLUBS
 */
public record Card(Rank rank, Suit suit) implements Comparable<Card> {

    /**
     * method that returns card representation in format: 'Rank of Suit',
     * for example 'ACE of Spades'
     * @return rank in string format
     */
    @Override
    public String toString(){
        return rank.toString() + " " + suit.toString();
    }

    /**
     * tells us if cards are the same or not (based on the rank and suit)
     * @param o the reference object with which to compare.
     * @return {@code true} if cards are the same or {@code false} if cards are different
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return rank ==  card.rank && suit == card.suit;
    }

    /**
     * return hashCode value for the object
     * @return hashCode value
     */
    @Override
    public int hashCode() {
        return Objects.hash(rank, suit);
    }

    /**
     * compares the other card with another card based on value ( ACE - 14, KING - 13, QUEEN - 12, ...)
     * @param other card with which we compare
     * @return positive value -> if the card is higher, zero -> if the card is the same
     * or negative value -> if the card is lower than the card that we compare to,
     */
    @Override
    public int compareTo(Card other) {
        return Integer.compare(this.rank.getValue(), other.rank.getValue());
    }


}
