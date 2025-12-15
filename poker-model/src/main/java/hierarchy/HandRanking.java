package hierarchy;

/**
 * An enum class that contains of all the possible string layouts of player's hand and a power of them
 */
public enum HandRanking {
    /**
     * the lowest layout that represents the highest value of the card
     */
    HIGH_CARD("High Card", 1),
    /**
     * A layout of one pair of cards of the same value (for example two ACES)
     */
    PAIR("Pair", 2),
    /**
     * A layout of two pairs of cards of the same value (for example two ACES and two FIVES)
     */
    TWO_PAIRS("Two Pairs", 3),
    /**
     * A layout of three cards of the same value (for example three ACES)
     */
    THREE_OAK("Three Of A Kind", 4),
    /**
     * A layout of five cards every card is on value lower thea the previous one (for example TEN, NINE, EIGHT, SEVEN, SIX)
     */
    STRAIGHT("Straight", 5),
    /**
     * A layout of five cards in the same color (for example five cards in HEARTS)
     */
    FLUSH("Flush", 6),
    /**
     * A layout of one pair of cards of the same value and three other cards of the same value (for example two ACES and three JACKS)
     */
    FULL_HOUSE("Full House", 7),
    /**
     * A layout of four cards of the same value (for example four ACES)
     */
    FOUR_OAK("Four Of A Kind", 8),
    /**
     * A layout of five cards every card is on value lower thea the previous one
     * and in the same suit
     * (for example TEN, EIGHT, QUEEN, JACK, NINE in SPADES, HEARTS, CLUBS OR DIAMONDS)
     */
    STRAIGHT_FLUSH("Straight Flush", 9),
    /**
     * A layout of five cards every card is on value lower than the previous one
     * but starting from ACE
     */
    ROYAL_FLUSH("Royal Flush", 10);

    /**
     * A private final string name of teh layout (for example 'Full House')
     */
    private final String name;
    /**
     * A private final int value of the layout (for example for Straight -> 5)
     */
    private final int value;

    /**
     * A constructor that sets name and value to the hand ranking
     * @param name of the layout
     * @param value of the layout
     */
    HandRanking(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * getter
     * @return value of the layout in hand
     */
    public int getValue() {
        return value;
    }

    /**
     * A method that changes name to string format
     * @return name in string format
     */
    @Override
    public String toString() {
        return name;
    }

}
