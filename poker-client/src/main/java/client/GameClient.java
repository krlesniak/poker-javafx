package client;

import common.CommandParser;
import common.GameCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that represents the network client for the Poker Game, utilizing blocking I/O (sockets and streams)
 * to communicate with the non-blocking NioPokerServer
 * This class operates using a two-thread model: the main thread handles user input and sending commands and
 * a separate worker thread listens for incoming messages from the server.
 */
public class GameClient {
    /**
     * Static LOGGER for GameClient
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GameClient.class);
    /**
     * A private final string host address of the Poker Server.
     */
    private final String host;
    /**
     * A private final int port number (for example 7777) used for connection.
     */
    private final int port;
    /**
     * A private primary network socket connecting the client to the server.
     */
    private Socket socket;
    /**
     * A private output stream writer used for sending text messages to the server.
     */
    private PrintWriter out;
    /**
     * A private input stream reader used for receiving messages from the server.
     */
    private BufferedReader in;
    /**
     * A private boolean flag indicating whether the client connection is currently active.
     */
    private boolean running;

    /**
     * A constructor that constructs a Client.GameClient instance with the specified connection details.
     * @param host the hostname or IP address of the server.
     * @param port the port number of the server
     */
    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * A method that starts the socket connection, initializes I/O streams, and starts the dedicated
     * listener thread to handle incoming server messages.
     * @throws IOException If a network or I/O error occurs during connection setup.
     */
    public void start() throws IOException {
        // connecting to the server
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;

        LOGGER.info("        Connected to {} server: {}", host, port);

        new Thread(this::listenFromServer).start();
    }

    /**
     * A method that sends a message (command) from the client to the server, appending a newline character ('\n')
     * which serves as the protocol delimiter.
     * @param message The command string (for example "BET 50", "JOIN game-id name") to be sent.
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message); // sends text + '\n\
        }
    }

    /**
     * A method with the listener thread loop.
     * It reads lines from the server using blocking I/O until disconnected.
    */
    private void listenFromServer() {
        try {
            String line;
            // loop that blocks readLine(), waiting for '\n'
            while (running && (line = in.readLine()) != null) {
                processServerMessage(line);
            }
        } catch (IOException e) {
            LOGGER.info("Disconnected from server");
        } finally {
            close();
        }
    }

    /**
     * A method that processes a single message received from the server.
     * It parses the message into a GameCommand and handles specific UI output for game events.
     * @param message The raw protocol string received from the server.
     */
    private void processServerMessage(String message) {
        // parsing command from server
        GameCommand cmd = CommandParser.parse(message);

        LOGGER.info("[SERVER]: {}", message);

        if (cmd.getType() == common.CommandType.DEAL) {
            LOGGER.info(" -=-=-=-=-=-=- YOU RECEIVED CARDS TO YOUR HAND! Check them by typing SHOW -=-=-=-=-=-=-");
        }
    }

    /**
     * A method that closes the client connection by setting the running flag to false
     * and closing the network socket.
     */
    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // empty
        }
    }
}
