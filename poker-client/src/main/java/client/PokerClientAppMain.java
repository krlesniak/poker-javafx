package client;

import java.io.IOException;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main application class and entry point for the Poker Game Client.
 * This class is responsible for initializing the Client.GameClient, managing the console
 * user interface, reading commands from the keyboard, and sending them to the server.
 */
public class PokerClientAppMain {
    /**
     * Static LOGGER for PokerClientAppMain
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PokerClientAppMain.class);

    /**
     * The default hostname or IP address of the Poker Server.
     */
    // localhost
    private static final String SERVER_HOST = "localhost";
    /**
     * The default port number used to connect to the Poker Server.
     */
    private static final int SERVER_PORT = 7777;

    /**
     * Constructs the main client application entry point.
     */
    public PokerClientAppMain() {
        // empty constructor defined by program
    }

    /**
     * The main method that starts the client application.
     * It handles the setup of the connection and manages user input trough Socket.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        GameClient client = new GameClient(SERVER_HOST, SERVER_PORT);

        try {
            // start connection womp womp checking for git push
            client.start();

            // reading from keyboard
            Scanner scanner = new Scanner(System.in);
            LOGGER.info("---------------------------------------------------------------------------");
            LOGGER.info("Enter a command (for example HELLO 1.0, CREATE 10 50, JOIN game-id name.");
            LOGGER.info("After creating a game with at least two players start a game by using START command.");
            LOGGER.info("For other help type command HELP with instruction.");
            LOGGER.info("---------------------------------------------------------------------------");

            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();

                if ("EXIT".equalsIgnoreCase(input.trim())) {
                    client.sendMessage("QUIT");
                    break;
                }

                // sending raw text to server
                client.sendMessage(input);
            }

            client.close();

        } catch (IOException e) {
            LOGGER.error("Could not connect to the server: {}", e.getMessage(),e);
        }
    }
}
