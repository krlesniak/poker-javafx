package exceptions;

/**
 * Exception thrown when a player attempts to exchange cards that does not match
 * the five cards poker rules.
 */
public final class IllegalDrawException extends InvalidMoveException {
    /**
     * A method that creates a new IllegalDrawException with a specified message
     * describing the violation.
     * @param message that shows which draw rules were broken
     */
    public IllegalDrawException(String message) {
        super("ILLEGAL_DRAW", message);
    }
}