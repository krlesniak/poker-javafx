package client;

import common.CommandParser;
import common.GameCommand;
import javafx.application.Platform;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameClient.class);
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean running;

    // NOWE: Słuchacz dla GUI
    private Consumer<GameCommand> onCommandReceived;

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setOnCommandReceived(Consumer<GameCommand> handler) {
        this.onCommandReceived = handler;
    }

    public void start() throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;
        new Thread(this::listenFromServer).start();
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    private void listenFromServer() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                processServerMessage(line);
            }
        } catch (IOException e) {
            LOGGER.info("Disconnected");
        }
    }

    private void processServerMessage(String message) {
        GameCommand cmd = CommandParser.parse(message);
        // Przesyłamy komendę do wątku JavaFX
        if (onCommandReceived != null) {
            Platform.runLater(() -> onCommandReceived.accept(cmd));
        }
    }

    public void close() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }
}