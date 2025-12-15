package exceptions;

/**
 * Exception thrown when a player tries to give more chips then he has on his hand
 */
public final class NotEnoughChipsException extends InvalidMoveException {
    /**
     * A constructor that makes a NotEnoughChipsException
     * @param required amount of chips that is required to attempt in the bet
     * @param current amount that you have and is not enough to play
     */
    public NotEnoughChipsException(int required, int current) {
        super("NO_CHIPS", "Lack of chips. Required: " + required + ", you have: " + current);
    }
}
