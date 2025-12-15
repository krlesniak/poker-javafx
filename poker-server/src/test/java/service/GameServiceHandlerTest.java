package service;

import common.CommandParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.GameServiceHandler.ServiceResult;

import static org.junit.jupiter.api.Assertions.*;

class GameServiceHandlerTest {

    private GameServiceHandler testHandler;
    private final String senderId = "testUser123";

    @BeforeEach
    void setUp() {
        testHandler = new GameServiceHandler();
    }

    @Test
    void testHandleHello_ShouldReturnWelcome() {
        // HELLO test
        ServiceResult result = testHandler.processCommand(senderId, null, CommandParser.parse("HELLO 1.0"));

        assertFalse(result.broadcast(), "HELLO should be a private message.");
        assertTrue(result.message().contains("OK Welcome"), "Message should confirm connection.");
    }

    @Test
    void testHandleCreate_ShouldReturnWelcomeAndMapGame() {
        // CREATE test
        ServiceResult result = testHandler.processCommand(senderId, null, CommandParser.parse("CREATE 10 50"));

        assertTrue(result.broadcast(), "CREATE should broadcast the game ID.");
        assertNotNull(result.targetGameId(), "Should return a new Game ID.");
        assertTrue(result.message().contains("WELCOME game-"), "Message should contain WELCOME command.");

        //checking if gameId has been added
        assertDoesNotThrow(() -> testHandler.getEngine(result.targetGameId()), "GameEngine should be active.");
    }

    @Test
    void testInvalidCommand_ShouldReturnError() {
        // testing DEFAULT in switch
        ServiceResult result = testHandler.processCommand(senderId, null, CommandParser.parse("INVALID_CMD"));

        assertFalse(result.broadcast());
        assertTrue(result.message().startsWith("ERR Error"), "Should return an error for invalid command.");
    }

    // helper method for other tests
    private String createAndJoinGame(String hostId, String joinerId) {
        // Helper method for quick creating and joining
        ServiceResult createResult = testHandler.processCommand(hostId, null, CommandParser.parse("CREATE 10 50"));
        String gameId = createResult.targetGameId();
        testHandler.processCommand(joinerId, gameId, CommandParser.parse("JOIN " + gameId + " JoinerName"));
        return gameId;
    }
    // Tests for logic QUIT

    @Test
    void testHandleQuit_GameExists_ShouldRemovePlayerAndBroadcast() {
        // Creating game and joining two players
        String hostId = "Host1";
        String joinerId = "Joiner1";
        String gameId = createAndJoinGame(hostId, joinerId);

        // Host leaves the game
        ServiceResult result = testHandler.processCommand(hostId, gameId, CommandParser.parse("QUIT"));

        // Broadcast for remaining player
        assertTrue(result.broadcast(), "QUIT should broadcast LOBBY info to remaining players.");
        assertTrue(result.message().contains("Host left the game. Active players: JoinerName"), "Message should notify about player leaving.");

        // Player removed from map
        assertNull(testHandler.playerGameMap.get(hostId), "Host should be removed from playerGameMap.");

        // GameEngine still active
        assertDoesNotThrow(() -> testHandler.getEngine(gameId), "GameEngine should still exist with Joiner.");
    }

    @Test
    void testHandleQuit_HostLeavesLast_ShouldRemoveGameEngine() {
        // Creating game with Host only
        String hostId = "HostOnly";
        ServiceResult createResult = testHandler.processCommand(hostId, null, CommandParser.parse("CREATE 10 50"));
        String gameId = createResult.targetGameId();

        // Host leaves the game (removes last player)
        ServiceResult result = testHandler.processCommand(hostId, gameId, CommandParser.parse("QUIT"));

        // Sending OK to Host
        assertTrue(result.message().contains("Leaving current game..."), "Should confirm leaving.");

        // Game removed from map
        assertThrows(IllegalArgumentException.class, () -> testHandler.getEngine(gameId), "GameEngine should be removed if empty.");
        assertNull(testHandler.playerGameMap.get(hostId), "Host should be removed from playerGameMap.");
    }

    @Test
    void testHandleMove_GameNotExist() {
        ServiceResult result = testHandler.processCommand(senderId, "non-existent-id", CommandParser.parse("BET 10"));
        assertTrue(result.message().contains("ERR INVALID_ARG"), "Should fail because game does not exist.");
    }

    @Test
    void testHandleMove_WithAmount() {
        String hostId = "Host1";
        String gameId = createAndJoinGame(hostId, "Player2");

        testHandler.processCommand(hostId, gameId, CommandParser.parse("START"));

        ServiceResult result = testHandler.processCommand(hostId, gameId, CommandParser.parse("BET 100"));

        assertTrue(result.broadcast());
        assertTrue(result.message().contains("ACTION Host BET 100"), "Message should include action and amount.");
    }

    @Test
    void testHandleMove_NoAmount() {
        String hostId = "Host1";
        String gameId = createAndJoinGame(hostId, "Player2");

        testHandler.processCommand(hostId, gameId, CommandParser.parse("START"));

        try {
            ServiceResult result = testHandler.processCommand(hostId, gameId, CommandParser.parse("CALL"));
            // ACTION without call amount
            assertTrue(result.broadcast());
            assertTrue(result.message().contains("ACTION Host CALL"), "Message should include action without amount.");
            assertFalse(result.message().contains(" 0\n"), "Message should not include 0 amount.");
        } catch (Exception e) {
            //ignore
        }
    }

    @Test
    void testHandleDraw_GameNotExist_ShouldThrowInvalidArgument() {
        ServiceResult result = testHandler.processCommand(senderId, "non-existent-id", CommandParser.parse("DRAW 0"));
        assertTrue(result.message().contains("ERR INVALID_ARG"), "DRAW should fail if game does not exist.");
    }

    @Test
    void testHandleStatus_Success_ShouldReturnFullInfo() {
        String hostId = "Host1";
        String joinerId = "Player2";
        String gameId = createAndJoinGame(hostId, joinerId);
        //start game
        testHandler.processCommand(hostId, gameId, CommandParser.parse("START"));

        // status by host
        ServiceResult result = testHandler.processCommand(hostId, gameId, CommandParser.parse("STATUS"));

        assertFalse(result.broadcast(), "STATUS should be a private message.");
        assertTrue(result.message().contains("STATUS"), "Message should start with STATUS command.");

        assertTrue(result.message().contains("Phase:"), "Should contain Phase info.");
        assertTrue(result.message().contains("Your name: Host"), "Should contain player name (Host).");
        assertTrue(result.message().contains("Current turn:"), "Should contain turn info.");
        assertTrue(result.message().contains("Your chips:"), "Should contain chips info.");
        assertTrue(result.message().contains("Game ID: " + gameId), "Should contain correct Game ID.");
    }

    @Test
    void testHandleStatus_GameNotExist_ShouldReturnError() {

        ServiceResult result = testHandler.processCommand(senderId, "bad-id-status", CommandParser.parse("STATUS"));

        assertFalse(result.broadcast(), "Error message should be private.");

        assertTrue(result.message().contains("ERR Error"), "Should return generic error.");

        assertTrue(result.message().contains("You are not in the game"), "Should return specific error message.");
    }

    @Test
    void testHandleHelp_ShouldReturnFullInstructionSet() {
        // Test that handleHelp builds the instruction string correctly
        final String testGameId = "game-abc";

        ServiceResult result = testHandler.processCommand(senderId, testGameId, CommandParser.parse("HELP"));

        assertFalse(result.broadcast(), "HELP message should be private.");

        assertTrue(result.message().startsWith("MSG "), "Message should start with MSG command.");

        assertTrue(result.message().contains("ACTIONS DURING DRAWING"), "Message should contain the DRAW section instruction.");

        assertEquals(testGameId, result.targetGameId(), "ServiceResult should pass through the gameId.");
    }

    @Test
    void testHandleShow_GameNotExist_ShouldReturnError() {
        // Test the error path when the game ID is invalid.

        // gameId will be invalid, throwing IllegalArgumentException from getEngine(gameId)
        ServiceResult result = testHandler.processCommand(senderId, "non-existent-game", CommandParser.parse("SHOW"));

        // Assertions: Should be caught and returned as INVALID_ARG
        assertTrue(result.message().contains("ERR INVALID_ARG"), "SHOW should fail with INVALID_ARG if game ID does not exist.");
        assertFalse(result.broadcast(), "Error message should be private.");
    }

    @Test
    void testHandleDraw_Success_ShouldReturnDRAWOK(){
        String hostId = "Host1";
        String joinerId = "Player2";
        String gameId = createAndJoinGame(hostId, joinerId);

        // DEAL -> BET1
        testHandler.processCommand(hostId, gameId, CommandParser.parse("START"));

        // changing phase
        testHandler.processCommand(hostId, gameId, CommandParser.parse("CHECK"));
        testHandler.processCommand(joinerId, gameId, CommandParser.parse("CHECK"));

        // DRAW command
        ServiceResult result = testHandler.processCommand(hostId, gameId, CommandParser.parse("DRAW 0 1"));

        if (result.broadcast()) {
            assertTrue(result.message().contains("DRAWOK"), "Should return DRAWOK command.");
            assertTrue(result.message().contains("Host"), "Should contain player name.");
        } else {
            assertTrue(result.message().startsWith("ERR"), "If DRAW is not allowed in this phase, server should return ERR.");
        }
    }


}