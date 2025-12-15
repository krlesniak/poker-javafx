package exceptions;

/**
 * Exception thrown when a player tries to make na illegal move
 */
public class InvalidMoveException  extends RuntimeException {
    /**
     * An error code (for example 'NO_CHIPS') that is being sent by server to the client
     */
    private final String code;

    /**
     * A constructor that makes an IllegalMoveException
     * @param code that will be shown to the client
     * @param message a specific text explains the reason of the exception
     */
    public InvalidMoveException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * A method that returns an error code
     * @return code that will be shown to the client
     */
    public String getCode() {
        return code;
    }


}
