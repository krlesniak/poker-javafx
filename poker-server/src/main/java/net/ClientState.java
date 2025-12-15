package net;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.ArrayDeque;


/**
 * A class that stores the network connection state with a client.
 */
public class ClientState {
    /**
     * A public string player's id set after joining
     */
    private String playerId = null;

    /**
     * A public string game's id before creating it
     */
    private String gameId = null;

    /**
     * Buffer for input data
     */
    public final ByteBuffer readBf =  ByteBuffer.allocate(4096);

    /**
     * A public final queue of messages to be sent (if the socket does not accept everything at once)
     */
    public final Queue<ByteBuffer> writeQ = new ArrayDeque<>();

    /**
     * An empty constructor
     */
    public ClientState() {
        // empty constructor defined by program
    }

    /**
     * Getter for the player's unique network identifier.
     * @return the player's ID.
     */
    public String getPlayerId() {
        return playerId;
    }

    /**
     * Setter for the player's unique network identifier.
     * @param playerId the new ID to set after joining/creating.
     */
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    /**
     * Getter for the ID of the game the client is currently in.
     * @return the game ID.
     */
    public String getGameId() {
        return gameId;
    }

    /**
     * Setter for the ID of the game the client is currently in.
     * @param gameId the new game ID.
     */
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

}
