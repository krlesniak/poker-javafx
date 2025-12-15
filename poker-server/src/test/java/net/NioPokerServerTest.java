package net;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class NioPokerServerTest {

    // another test port
    private static final int TEST_PORT = 8888;
    private NioPokerServer testServer;
    private ExecutorService testServerExecutor;

    @BeforeEach
    void setUp(){
        testServer = new NioPokerServer(TEST_PORT);
        testServerExecutor = Executors.newSingleThreadExecutor();

        // starting server in another thread
        testServerExecutor.submit(() -> testServer.run());

        // Attempting to connect to the server
        final int MAX_ATTEMPTS = 20;

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            try (Socket ignored = new Socket("localhost", TEST_PORT)) {
                //success server is ready
                return;
            } catch (IOException e) {
                // Connection refused
                Thread.yield();
            }
        }
        // error when we did not succeed
        throw new RuntimeException("Server did not start within the allowed timeout.");
    }

    @AfterEach
    void tearDown() {
        // shutting down the server instantly
        testServerExecutor.shutdownNow();
    }

    @Test
    void testConnectionAndHello() throws IOException {
        try (Socket client = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(client.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            // testing HELLo command
            out.println("HELLO 1.0");
            String response = in.readLine();

            assertNotNull(response);
            assertTrue(response.contains("OK Welcome"));
        }
    }

    @Test
    void testGameAndBroadcastAndDeal() throws IOException {
        // tests for methods: handleCreate, handleJoin, handleStart, sendDealMessages, broadcastToGame

        // HOST (player 1)
        try (Socket host = new Socket("localhost", TEST_PORT);
             PrintWriter hostOut = new PrintWriter(host.getOutputStream(), true);
             BufferedReader hostIn = new BufferedReader(new InputStreamReader(host.getInputStream()));

             // JOINER (player 2)
             Socket player2 = new Socket("localhost", TEST_PORT);
             PrintWriter joinerOut = new PrintWriter(player2.getOutputStream(), true);
             BufferedReader joinerIn = new BufferedReader(new InputStreamReader(player2.getInputStream()))) {

            // HOST creates the game
            hostOut.println("CREATE 10 50");
            String hostResp = hostIn.readLine(); // WELCOME game-id
            assertTrue(hostResp.contains("WELCOME"));
            String gameId = hostResp.split(" ")[1]; // taking game id on index 1

            // Joiner joins the game and appears broadcast LOBBY
            joinerOut.println("JOIN " + gameId + " Player2");

            // Joiner get info about the LOBBY
            String joinerResp = joinerIn.readLine();
            assertTrue(joinerResp.contains("LOBBY"));

            // Host also gets ingo about yhe LOBBY
            String broadcastToHost = hostIn.readLine();
            assertTrue(broadcastToHost.contains("LOBBY"));

            // Host starts the game (tests for handleStart and sendMessageDeal)
            hostOut.println("START");

            // New responses from server that should appear
            // DEAL as private message with cards
            // STARTED as broadcast
            // INFO about phase as broadcast
            // TURN as broadcast

            // checking cards and turn for host
            boolean dealReceived = false;
            boolean turnReceived = false;

            final long TIMEOUT_MS = 2000;
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
                // reads all available lines
                while (hostIn.ready()) {
                    String line = hostIn.readLine();
                    if (line == null) {
                        // connection lost
                        break;
                    }

                    if (line.startsWith("DEAL")) dealReceived = true;
                    if (line.startsWith("TURN")) turnReceived = true;
                }
                if (dealReceived && turnReceived) {
                    break;
                }
                // minimal time wait not to load the server
                Thread.yield();
            }
            assertTrue(dealReceived, "Host should receive DEAL message (cards)");
            assertTrue(turnReceived, "Host should receive TURN message");
        }
    }

    @Test
    void testMessageAndQuit() throws IOException {
        try (Socket client = new Socket("localhost", TEST_PORT);
             PrintWriter out = new PrintWriter(client.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            // two commands in one line (test for handleRead)
            out.print("HELLO 1.0\nSTATUS\n");
            out.flush();

            String resp1 = in.readLine(); // response for HELLO 1.0
            assertNotNull(resp1);

            String resp2 = in.readLine(); // response for STATUS
            assertNotNull(resp2);

            // testing QUIT command (checks if processServiceResult resets gameId after quiting a game)
            out.println("QUIT");
            String quitResp = in.readLine();
            assertTrue(quitResp.contains("OK"));
        }
    }
}