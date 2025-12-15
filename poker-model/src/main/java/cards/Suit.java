package cards;

/**
 * An enum class Suit that collects of the possible colors of card
 * (SPADES, CLUBS, HEARTS, DIAMONDS) and assign to them their string name
 */
public enum Suit {
    /**
     * Suit Spades
     */
    SPADES("♠"),
    /**
     * Suit Clubs
     */
    CLUBS("♣"),
    /**
     * Suit Hearts
     */
    HEARTS("♥"),
    /**
     * Suit Diamonds
     */
    DIAMONDS("♦");

    /**
     * a variable that represents a symbol of every card
     */
    private final String symbol;

    /**
     * A constrcutor Suit that assigns symbol text to the variable
     * @param symbol assigned to teh variable
     */
    Suit(String symbol) {
        this.symbol = symbol;
    }

    /**
     * A method that is responsible for representing symbol in string format
     * @return symbol in string format
     */
    @Override
    public String toString() {
        return symbol;
    }
}