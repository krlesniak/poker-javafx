package common;

/**
 * A class that contains every possible command that can be sent from
 * client to the server or from server to the client
 */
public enum CommandType {

    // -=-=-=-=-=-=- CLIENT -> SERVER -=-=-=-=-=-=-
    /**
     * introducing clients and showing version | usage : HELLO 1.0
     */
    HELLO,
    /**
     * creating new game | usage : CREATE 10 50 (ANTE = 10, BET_MIN = 50)
     */
    CREATE,
    /**
     * joining the game
     */
    JOIN,
    /**
     * leaving the game
     */
    LEAVE,
    /**
     * starting the game
     */
    START,
    /**
     * quiting the game
     */
    QUIT,
    /**
     * showing info about the players
     */
    STATUS,

    // GAMEPLAY
    /**
     * betting | usage : BET 100
     */
    BET,
    /**
     * equalize to the amount in the others players bet
     */
    CALL,
    /**
     * waiting not adding anything to the pool
     */
    CHECK,
    /**
     * stop betting in round
     */
    FOLD,
    /**
     * exchange cards | usage : DRAW 0 1 4 [indexes]
     */
    DRAW,
    /**
     * showing cards
     */
    SHOW,

    //  -=-=-=-=-=-=- SERVER -> CLIENT -=-=-=-=-=-=-
    /**
     * success
     */
    OK,
    /**
     * error | usage : ERR *error*
     */
    ERR,
    /**
     * at the beginning | usage : WELCOME game1 player12
     */
    WELCOME,

    // LOBBY PHASE
    /**
     * list of players in the game | usage : LOBBY playerA playerB
     */
    LOBBY,

    // GAMEPLAY
    /**
     * game began | usage : STARTED 10 50 (ANTE, BET_MIN)
     */
    STARTED,
    /**
     * giving cards | usage : DEAL A,J,...
     */
    DEAL,
    /**
     * whose turn | usage : TURN playerB BET1 50
     */
    TURN,

    /**
     * info about other player action | usage : ACTION playerA BET 100
     */
    // others player turn info
    ACTION,
    /**
     * info that sbd exchanged their cards | usage : DRAWOK playerA 3
     */
    DRAWOK,

    // RESULTS
    /**
     * the end of betting | usage : ROUND 250 100 (250 -> in the pool, 100 -> the highest bet)
     */
    ROUND,
    /**
     * showing cards | usage : SHOWDOWN playerA ... (cards)
     */
    SHOWDOWN,
    /**
     * who is the winner | usage : WINNER playerA 500 FULL_HOUSE
     */
    WINNER,
    /**
     * chips cashout | usage : PAYOUT playerA 500 1200 (won 500 and now have 1200)
     */
    PAYOUT,
    /**
     * the end of the game | usage : END NORMAL (normal end)
     */
    END,
    /**
     * shows an instruction how to use every command
     */
    HELP,
    /**
     * text message / instruction
     */
    MSG,
    RESTART,
    DEALER,
    LOG,
    STRENGTH,
    FULL_RESET,
    READY_SHOWDOWN,
    REVEAL,
}
