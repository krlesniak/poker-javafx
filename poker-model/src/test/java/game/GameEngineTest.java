package game;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import exceptions.*;
import players.Player;
import cards.Card;
import cards.Rank;
import cards.Suit;

class GameEngineTest {
    private GameEngine testEngine;
    private Player testPlayer1;
    private Player testPlayer2;

    @BeforeEach
    void setUp() {
        testEngine = new GameEngine("game1", 20, 50);
        testPlayer1 = new Player("pl1", "John", 1000);
        testPlayer2 = new Player("pl2", "Pork", 1000);

        testEngine.addPlayer(testPlayer1);
        testEngine.addPlayer(testPlayer2);
    }

    @Test
    void testStartGame() {
        testEngine.startGame();

        assertEquals(GameState.BET1, testEngine.getState());

        assertEquals(testEngine.getPool(), 40); // 20 per player == 40

        // 1000 - 20 = 980 chips after ante
        assertEquals(testPlayer1.getChips(), 980);
        assertEquals(testPlayer2.getChips(), 980);

        // after dealing every player should have 5 cards each
        assertEquals(testPlayer1.getHand().size(), 5);
        assertEquals(testPlayer2.getHand().size(), 5);
    }

    @Test
    void testHandleBet(){
        testEngine.startGame();

        testEngine.handleBetMove(testPlayer1, "BET", 50);

        assertEquals(GameState.BET1, testEngine.getState());

        assertEquals(testEngine.getPool(), 90); // ante + bet
        assertEquals(testEngine.getCurrentBet(), 50);

        testEngine.handleBetMove(testPlayer2, "CALL", 50);

        assertEquals(testEngine.getPool(), 140);
        assertEquals(GameState.DRAW, testEngine.getState());

    }

    @Test
    void testHandlePayout(){
        testEngine.startGame();

        setUp();
        testEngine.startGame();

        testEngine.handleBetMove(testPlayer1, "BET", 100);

        testEngine.handleBetMove(testPlayer2, "FOLD", 0);

        // payout phase
        assertEquals(GameState.PAYOUT, testEngine.getState());

        testEngine.handlePayout();

        // after payout end
        assertEquals(GameState.END, testEngine.getState());

        // winning only opponents ante because he folded instantly
        assertEquals(1020, testPlayer1.getChips());

        assertEquals(980, testPlayer2.getChips());
    }
}