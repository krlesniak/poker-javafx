package exceptions;

/**
 * Exception thrown when the server detects an action that violates
 * the security of the game state, for example
 * cheating or abuse
 */
public final class SecurityException extends InvalidMoveException {
    /**
     * A constructor that makes a SecurityException
     * @param reason description of the detected security violation
     */
    public SecurityException(String reason) {
        super("SECURITY_VIOLATION", "Something dangerous may happen: " + reason);
    }
}
