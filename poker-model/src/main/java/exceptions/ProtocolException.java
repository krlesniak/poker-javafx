package exceptions;

/**
 * Exception thrown when the server receives a network message
 * that is incorrect or incomplete
 */
public final class ProtocolException extends InvalidMoveException {
    /**
     * A constructor that makes a ProtocolException
     * @param receivedMessage the raw message string received from the client.
     * @param reason a description of why the protocol violation occurred
     */
    public ProtocolException(String receivedMessage, String reason) {
        super("PROTOCOL_ERROR", "Invalid data format: " + reason + ". Got: " + receivedMessage);
    }
}
