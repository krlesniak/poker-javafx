package exceptions;

/**
 * Exception thrown when a player tries to make an action (BET, FOLD, CHECK) when it is not
 * his / her turn
 */
public final class OutOfTurnException extends InvalidMoveException{
    /**
     * A constructor that makes an OutOfTurnException
     * it returns a code of the protocol and a message to the client
     */
    public OutOfTurnException() {
        super("OUT_OF_TURN", "It is not your turn.");
    }
}
