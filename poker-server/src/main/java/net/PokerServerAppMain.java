package net;

/**
 * A main application class and entry point for the Poker Game Server.
 * This class initializes and starts the NioPokerServer on a defined network port,
 * launching the  loop for handling all client connections and game commands.
 */
public class PokerServerAppMain {
    /**
     * Constructs the PokerServerAppMain instance.
     * This constructor is empty
     */
    public PokerServerAppMain() {
        // empty constructor defined by program
    }
    /**
     * The main method that starts the application.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        // for example port 7777
        NioPokerServer server = new NioPokerServer(7777);
        server.run();
    }
}