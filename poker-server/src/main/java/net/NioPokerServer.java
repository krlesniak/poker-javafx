package net;
import common.CommandParser;
import common.GameCommand;
import common.CommandType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import service.GameServiceHandler;
import players.Player;
import game.GameEngine;
import cards.Card;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that represents the core server implementation using Java NIO.
 * This class runs the loop, managing all client connections,
 * reading incoming commands, and sending responses.
 */
public class NioPokerServer implements Runnable {

    /**
     * Static LOGGER for NioPokerServer
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NioPokerServer.class);
    /**
     * A private final network port number on which the server is listening for incoming client connections.
     */
    private final int port;
    /**
     * A private selector multiplexer used for managing non-blocking I/O operations.
     * It monitors sockets for reading, writing, or accepting connections.
     */
    private Selector selector;
    /**
     * A private final core part responsible for handling the business logic of the poker game.
     * All received commands are delegated to this handler for processing.
     */
    private final GameServiceHandler gameHandler = new GameServiceHandler();

    /**
     * Flag controlling the main server loop execution. Set to true to stop the server gracefully.
     */
    private volatile boolean running = true;

    /**
     * A constructor that constructs the NIO Poker Server, adjusting it to the specified port.
     * @param port The port number on which the server should listen for connections.
     */
    public NioPokerServer(int port) {
        this.port = port;
    }

    /**
     * A main method that contains of the execution loop for the server.
     * It initializes the Selector, binds the server socket, and starts listening for events.
     */
    @Override
    public void run() {
        // Local declaration, required after field removal
        ServerSocketChannel serverChannel = null;

        try {
            // nio initialization
            selector = Selector.open();

            serverChannel = ServerSocketChannel.open(); // Initialization
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false); // not blocking mode

            // registering server
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            LOGGER.info("Server started at port: {}", port);

            // actions loop
            while (running) {
                // waiting for an action
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove(); //removing the key

                    processSelectedKey(key);
                }
            }

        } catch (IOException e) {
            // Logging FATAL errors during initialization or runtime
            LOGGER.error("FATAL SERVER ERROR: {}", e.getMessage(), e);
        } finally {
            // This block guarantees resources are closed, whether the loop finished or an error occurred.
            // Now using the local variable serverChannel for closing
            if (serverChannel != null) {
                try {
                    serverChannel.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close server channel: {}", e.getMessage());
                }
            }
            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                    LOGGER.warn("Failed to close selector: {}", e.getMessage());
                }
            }
            LOGGER.info("Server stopped gracefully.");
        }
    }

    /**
     * A method that processes a single ready SelectionKey, handling ACCEPT, READ, or WRITE events.
     * It catches IOException and delegates cleanup to closeConnection.
     * @param key The SelectionKey that is ready for an I/O operation.
     */
    private void processSelectedKey(SelectionKey key) {
        if (!key.isValid()) return;

        try {
            if (key.isAcceptable()) {
                handleAccept(key);
            } else if (key.isReadable()) {
                handleRead(key);
            } else if (key.isWritable()) {
                handleWrite(key);
            }
        } catch (IOException e) {
            // if client lost connection
            LOGGER.error("FATAL SERVER ERROR: {}", e.getMessage(),e);
            closeConnection(key);
        }
    }

    // -=-=--=-=-=- actions handling -=-=--=-=-=-

    /**
     * A method that handles an incoming connection acceptance event.
     * Accepts the connection, sets the client socket to non-blocking, and registers it for reading.
     * @param key The SelectionKey associated with the ServerSocketChannel.
     * @throws IOException If an I/O error occurs during acceptance.
     */
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept(); // acceptable
        client.configureBlocking(false);        // not blocking client

        LOGGER.info("New connection: {}", client.getRemoteAddress());

        // creating new state for a client and adding him new key
        ClientState state = new ClientState();
        client.register(selector, SelectionKey.OP_READ, state);
    }

    /**
     * A method that waits for incoming data reading events.
     * It reads data into the buffer and handles message framing (splitting messages by '\n').
     * @param key The SelectionKey associated with the client's SocketChannel.
     * @throws IOException If an I/O error occurs during reading.
     */
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();

        // reading data from net to the buffer
        int bytesRead = client.read(state.readBf);

        if (bytesRead == -1) {
            // client closed connection
            closeConnection(key);
            return;
        }

        // ending after '\n'
        // preparing buffer for reading
        state.readBf.flip();

        // searching for '\n'
        while (true) {
            // finding position of '\n'
            boolean foundLine = false;
            int limit = state.readBf.limit();
            int pos = state.readBf.position();
            int newlineIndex = -1;

            for (int i = pos; i < limit; i++) {
                if (state.readBf.get(i) == '\n') {
                    newlineIndex = i;
                    foundLine = true;
                    break;
                }
            }

            // not a line
            if (!foundLine) {
                break;
            }

            // there is a line
            // length of line
            int length = newlineIndex - pos;
            byte[] lineBytes = new byte[length];
            state.readBf.get(lineBytes); // reads bytes
            // skipping '\n' character
            state.readBf.get();

            // changing our command to string
            String rawMessage = new String(lineBytes, StandardCharsets.UTF_8).trim();

            if (!rawMessage.isEmpty()) {
                LOGGER.info("Received from client: {}", rawMessage);
                processCommand(key, rawMessage); // processing command
            }
        }
        state.readBf.compact();
    }

    /**
     * A method that handles writable events.
     * Sends messages from the client's queue until the queue is empty or the socket buffer is full.
     * @param key The SelectionKey associated with the client's SocketChannel.
     * @throws IOException If an I/O error occurs during writing.
     */
    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();

        // checking the queue
        ByteBuffer buffer = state.writeQ.peek();

        // turning of interest ops
        if (buffer == null) {
            // only OP_READ
            key.interestOps(SelectionKey.OP_READ);
            return;
        }

        // trying to send data
        client.write(buffer);

        // checking if whole buffer was sent
        if (!buffer.hasRemaining()) {
            // if yes we remove it from the queue
            state.writeQ.poll();
        }

        // if queue is empty we turn if OP_WRITE
        if (state.writeQ.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    /**
     * A method that sends personalized DEAL messages to every player in the round.
     * This logic is necessary after the START command to show card information privately.
     * @param gameId The ID of the game being started.
     * @param engine The GameEngine instance managing the game state.
     */
    private void sendDealMessages(String gameId, GameEngine engine) {
        // DEAL for every player
        for (SelectionKey key : selector.keys()) {
            // if player is in the game of gameId
            if (key.isValid() && key.attachment() instanceof ClientState clientState && gameId.equals(clientState.getGameId())) {
                Player receivingPlayer = getPlayerByClientState(clientState, engine);
                if (receivingPlayer != null) {
                    // generates DEAL command to that player
                    String cardString = formatHandForPlayer(receivingPlayer);
                    String dealMsg = CommandParser.createMessage(
                            CommandType.DEAL,
                            receivingPlayer.getName(),
                            cardString
                    );
                    sendMessage(key, dealMsg);
                }
            }
        }
        // sending command TURN that says whose turn it is to every player
        Player firstPlayer = engine.getPlayers().get(engine.getPlayersTurnIdx());
        String turnMsg = CommandParser.createMessage(
                CommandType.TURN,
                firstPlayer.getName(),
                engine.getState().name(),
                String.valueOf(engine.getCurrentBet())
        );
        broadcastToGame(gameId, turnMsg);
    }

    /**
    * A helper method to sendDealMessage that searches the game's player list for
     * the Player object corresponding to the ClientState's player ID.
    * @param clientState The ClientState holding the player's network ID.
    * @param engine The GameEngine instance containing the list of players.
    * @return The found Player object, or null if not found.
    */
    private Player getPlayerByClientState(ClientState clientState, GameEngine engine) {
        String playerId = clientState.getPlayerId();
        for (Player p : engine.getPlayers()) {
            if (p.getId().equals(playerId)) {
                return p; // player found
            }
        }
        return null;
    }

    /**
     * A helper method that formats a player's hand into a string.
     * This method is called privately to display a player's own cards.
     * @param player The player whose hand is being formatted.
     * @return string representation of the player's hand, separated with commas.
     */
    // formatting player's hand
    private String formatHandForPlayer(Player player) {
        // returning a list of player's card
        StringBuilder sb = new StringBuilder();
        sb.append(": \n[");
        for (Card card : player.getHand().getCards()) {
            sb.append(card.toString());
            sb.append(", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");
        return sb.toString();
    }

    // -=-=-=-=-=-=- APP LOGIC -=-=-=-=-=-=-

    /**
     * A method that parses a raw message and delegates command execution to the GameServiceHandler.
     * It manages client state updates and decides whether to send a private
     * response or a broadcast.
     * @param key The SelectionKey of the client who sent the command.
     * @param rawMessage The unparsed command string received.
     */
    private void processCommand(SelectionKey key, String rawMessage) {
        ClientState state = (ClientState) key.attachment();
        GameCommand cmd = CommandParser.parse(rawMessage);

        String tempId = String.valueOf(key.channel().hashCode());

        String senderId = (state.getPlayerId() != null) ? state.getPlayerId() : tempId;

        GameServiceHandler.ServiceResult result = gameHandler.processCommand(senderId, state.getGameId(), cmd);

        if (cmd.getType() == CommandType.START && state.getGameId() != null) {
            // after the start players receive their cards in hand
            GameEngine engine = gameHandler.getEngine(state.getGameId());
            sendDealMessages(state.getGameId(), engine);
        }

        processServiceResult(key, senderId, result, cmd);
    }

    /**
     * A private helper method that processes the result returned by the GameServiceHandler.
     * It manages client state updates (setting game ID) and directs the message
     * to either a broadcast or a private sender.
     * @param key The SelectionKey of the client who sent the command.
     * @param senderId The unique network ID of the client.
     * @param result The ServiceResult containing the message and delivery instructions.
     * @param cmd The original command sent by the client.
     */
    private void processServiceResult(SelectionKey key, String senderId, GameServiceHandler.ServiceResult result, GameCommand cmd) {
        ClientState state = (ClientState) key.attachment();

        // new game id
        if (result.targetGameId() != null && state.getGameId() == null) {
            state.setGameId(result.targetGameId());
            state.setPlayerId(senderId);
        }

        // if a player quits a game we reset his history so he can create w new game
        if (cmd.getType() == CommandType.QUIT) {
            state.setGameId(null);
        }

        if (result.broadcast() && result.targetGameId() != null) {
            broadcastToGame(result.targetGameId(), result.message());
        } else {
            sendMessage(key, result.message());
        }
    }

    /**
     * A method that broadcasts a message to all clients currently connected to the specified game.
     * @param gameId The ID of the game room to broadcast to.
     * @param message The message to send.
     */
    private void broadcastToGame(String gameId, String message) {
        // we iterate by every key in selector
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.attachment() instanceof ClientState otherState && gameId.equals(otherState.getGameId())) {
                sendMessage(key, message);
            }
        }
    }

    /**
     * A method that queues a message to be sent to a single client (identified by the SelectionKey).
     * @param key The SelectionKey of the target client.
     * @param message The message content.
     */
    private void sendMessage(SelectionKey key, String message) {
        ClientState state = (ClientState) key.attachment();

        if (!message.endsWith("\n")) {
            message += "\n";
        }

        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        state.writeQ.add(ByteBuffer.wrap(data));

        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup(); // to notice change
    }

    /**
     * A method that closes the connection and cancels the SelectionKey.
     * @param key The SelectionKey of the connection to close.
     */
    private void closeConnection(SelectionKey key) {
        try {
            key.channel().close();
            key.cancel();
        } catch (IOException e) { // ignore

        }
    }
}

