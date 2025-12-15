package client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

class GameClientTest {
    private ServerSocket testServer;
    private Thread testThread;
    private GameClient testClient;
    // other port
    private static final int TEST_PORT = 9999;

    @BeforeEach
    void setUp() throws IOException {
        //starting a server
        testServer = new ServerSocket(TEST_PORT);
        testThread = new Thread(() -> {
            try {
                //waiting for client to connect
                Socket clientSocket = testServer.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                // reading clients message and responding
                String line;
                while ((line = in.readLine()) != null) {
                    if ("HELLO".equals(line)) {
                        out.println("WELCOME"); // server's respond
                    }
                    if ("DEAL".equals(line)) {
                        out.println("DEAL player 5 Cards");
                    }
                }
            } catch (IOException e) {
                // ignoring closing
            }
        });
        testThread.start();
    }

    // closing server
    @AfterEach
    void tearDown() throws IOException {
        if (testClient != null) testClient.close();
        if (testServer != null) testServer.close();
    }

    @Test
    void testClientConnectionAndCommunication(){
        // creating a client
        testClient = new GameClient("localhost", TEST_PORT);

        // test method start
        assertDoesNotThrow(() -> testClient.start());

        // test sendMessage
        testClient.sendMessage("HELLO");

        // testing deal
        testClient.sendMessage("DEAL");

        // closing
        testClient.close();
    }

    @Test
    void testSendMessageWhenNotConnected() {
        GameClient notStartedClient = new GameClient("localhost", TEST_PORT);
        assertDoesNotThrow(() -> notStartedClient.sendMessage("TEST"));
    }

}