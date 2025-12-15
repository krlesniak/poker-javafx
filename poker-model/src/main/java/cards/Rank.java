package cards;

/**
 * An enum class that collects all the possible ranks of cards and
 * assign to them a value that allows the cards to be comparable with o
 */
public enum Rank {
    /**
     * e.g. two of spades
     */
    TWO(2),
    /**
     * e.g. three of spades
     */
    THREE(3),
    /**
     * e.g. four of spades
     */
    FOUR(4),
    /**
     * e.g. five of spades
     */
    FIVE(5),
    /**
     * e.g. six of spades
     */
    SIX(6),
    /**
     * e.g. seven of spades
     */
    SEVEN(7),
    /**
     * e.g. eight of spades
     */
    EIGHT(8),
    /**
     * e.g. nine of spades
     */
    NINE(9),
    /**
     * e.g. ten of spades
     */
    TEN(10),
    /**
     * e.g. jack of spades
     */
    JACK(11),
    /**
     * e.g. queen of spades
     */
    QUEEN(12),
    /**
     * e.g. king of spades
     */
    KING(13),
    /**
     * e.g. ace of spades
     */
    ACE(14);

    /**
     * A private final int value iof the card (e.g. ace)
     */
    protected final int value;

    /**
     * A constructor Rank that initializes value to the protected int value
     * @param value assigned to teh variable
     */
    Rank(int value) {
        this.value = value;
    }

    /**
     * getter taht returns value of value variable
     * @return value
     */
    public int getValue() {
        return value;
    }

    /**
     * A method that is responsible for representing Rank in string format
     * (for example 11 -> 'JACK', 12 -> 'QUEEN', 13 -> 'KING', 14 -> 'ACE')
     * @return value of the card in string format
     */
    @Override
    public String toString() {
        if (value >= 2 && value <= 10){
            return String.valueOf(value);
        }
        if (value == 11){
            return "J";
        }
        else if (value == 12){
            return "Q";
        }
        else if (value == 13){
            return "K";
        }
        else{
            return "A";
        }
    }
}