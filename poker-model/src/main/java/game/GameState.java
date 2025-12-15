package game;

/**
 * An enum class that defines all the possible states in the round of poker
 */
public enum GameState {
    /**
     * In this state players enter the lobby, and in other rounds it is the beginning of the round
     */
    LOBBY,
    /**
     * In this state GameEngine collects ante amount from all the players that is set at the beginning of the game
     */
    ANTE,
    /**
     * In this state every player receive five cards to their hands
     */
    DEAL,
    /**
     * In this state it is the first chance for players to make their bets
     */
    BET1,
    /**
     * In this state every player can exchange from 0 to five cards from their hand for new cards in the deck
     */
    DRAW,
    /**
     * In this state it is the second chance for players to make their bets even bigger
     */
    BET2,
    /**
     * In this state players show their hand and reveal the winner of the round
     */
    SHOWDOWN,
    /**
     * In this state the winner receives the winning pool
     */
    PAYOUT,
    /**
     * In this state the round ends and players prepare for the next round
     */
    END
}
