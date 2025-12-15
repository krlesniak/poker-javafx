package service;

import common.CommandParser;
import common.CommandType;
import common.GameCommand;
import exceptions.InvalidMoveException;
import game.GameEngine;
import game.GameState;
import players.Player;
import cards.Card;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class of server logic module it manages GameEngine instances.
 * It processes and handles every command that will be written in client terminal
 */
public class GameServiceHandler {
    /**
     * Static LOGGER for GameServiceHandler
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GameServiceHandler.class);
    /**
     * Map that stores active games in the same moment
     */
    private final Map<String, GameEngine> activeGames = Collections.synchronizedMap(new HashMap<>());

    /**
     * Helper map playerId -> gameId
     */
    public final Map<String, String> playerGameMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * A record used to package and return the necessary information to the network (NioPokerServer)
     * after processing a command.
     * @param message The protocol string (command + data) to be sent to the client.
     * @param broadcast true if the message should be sent to all players in the game (broadcast), false if private.
     * @param targetGameId The ID of the game related to the action (used by the server to update client state).
     */
    public record ServiceResult(String message, boolean broadcast, String targetGameId) {}

    /**
     * Constructs the GameServiceHandler instance.
     * This constructor is empty
     */
    public GameServiceHandler(){
        // empty constructor defined by program
    }

    /**
     * The main method that processes Client -> Server commands.
     * @param senderId client's id that sent a command
     * @param currentGameId id of a current game
     * @param cmd parsed command GameCommand.
     * @return ServiceResult server that says what to send and to who
     */
    public ServiceResult processCommand(String senderId, String currentGameId, GameCommand cmd) {
        String gameId = (currentGameId != null) ? currentGameId : playerGameMap.get(senderId);

        try {
            return switch (cmd.getType()) {
                case HELLO -> handleHello();
                case CREATE -> handleCreate(senderId, cmd);
                case JOIN -> handleJoin(senderId, cmd);
                case START -> handleStart(gameId);
                case BET, CALL, CHECK, FOLD -> handleMove(senderId, gameId, cmd);
                case DRAW -> handleDraw(senderId, gameId, cmd);
                case STATUS -> handleStatus(senderId, gameId);
                case SHOW -> handleShow(senderId, gameId);
                case HELP -> handleHelp(gameId);
                case QUIT -> handleQuit(senderId, gameId);
                default -> createErrorResult("Invalid command or game error");
            };
        } catch (InvalidMoveException | IllegalArgumentException e) {
            if (e instanceof InvalidMoveException invalidMoveException) {
                String errorCode = invalidMoveException.getCode();
                return createErrorResult(errorCode, e.getMessage());
            } else {
                String errorCode = "INVALID_ARG";
                return createErrorResult(errorCode, e.getMessage());
            }
        } catch (Exception e) {
            LOGGER.error("Uncaught logical error in GameServiceHandler: {}", e.getMessage(),e);
            return createErrorResult("SERVER_ERROR", "Something went wrong");
        }
    }

    /**
     * A private method that handles the initial HELLO command.
     * @return A private WELCOME message.
     */
    private ServiceResult handleHello() {
        // Client is saying hi
        return new ServiceResult(
                CommandParser.createMessage(CommandType.OK, "Welcome"),
                false, null
        );
    }

    /**
     * A private method that handles the CREATE command. Creates a new GameEngine, adds the sender as the host,
     * and maps the player to the new game ID.
     * @param senderId The ID of the host client.
     * @param cmd The CREATE command containing ante and initial bet amounts.
     * @return A WELCOME broadcast message informing all clients about the new game ID.
     */
    private ServiceResult handleCreate(String senderId, GameCommand cmd) {
        // getting ante and min bet
        int ante = cmd.getIntParam(0);
        int minBet = 0;

        if (cmd.getParameters().length > 1) {
            minBet = cmd.getIntParam(1);
        } else {
            minBet = ante; //if there is no snd param min bet equals ante (ante = minBet)
        }

        String newGameId = "game-" + UUID.randomUUID().toString().substring(0, 3);

        GameEngine engine = new GameEngine(newGameId, ante, minBet);
        Player host = new Player(senderId, "Host", 1000);

        engine.addPlayer(host);
        activeGames.put(newGameId, engine);
        playerGameMap.put(senderId, newGameId);

        return new ServiceResult(
                CommandParser.createMessage(CommandType.WELCOME, newGameId),
                true, newGameId
        );
    }

    /**
     * A private method that handles the JOIN command. Adds a player to an existing game.
     * @param senderId The ID of the joining client.
     * @param cmd The JOIN command containing the game ID and player name.
     * @return A LOBBY broadcast message with the updated list of players in the game.
     * @throws IllegalArgumentException if the specified game ID does not exist.
     */
    private ServiceResult handleJoin(String senderId, GameCommand cmd) {
        String gameId = cmd.getParameters()[0];
        String playerName = cmd.getParameters()[1];

        GameEngine engine = activeGames.get(gameId);
        if (engine == null) throw new IllegalArgumentException("That game does not exist");

        Player p = new Player(senderId, playerName, 1000);
        engine.addPlayer(p);
        // mapping player to the certain game
        playerGameMap.put(senderId, gameId);

        // LOBBY broadcast
        return new ServiceResult(
                CommandParser.createMessage(CommandType.LOBBY, getPlayerNames(engine)),
                true, gameId
        );
    }

    /**
     * A private method that handles the START command.
     * @param gameId The ID of the active game.
     * @return A STARTED broadcast message, instructing the network layer to proceed with the DEAL and TURN sequence.
     * @throws IllegalArgumentException if the client is not in a game.
     */
    private ServiceResult handleStart(String gameId) {
        GameEngine engine = activeGames.get(gameId);
        if (engine == null) throw new IllegalArgumentException("You are not in the game");

        // starting game
        engine.startGame(); // State BET1

        // STARTED info
        String startedMsg = CommandParser.createMessage(
                CommandType.STARTED,
                String.valueOf(engine.getAnteAmount())
        );

        // Phase info after started
        ServiceResult info = broadcastPhaseInfo(engine);

        // connecting STARTED and INFO
        return new ServiceResult(startedMsg + info.message(), true, gameId);
    }

    /**
     * A private method that handles the SHOW command. Displays the current cards held by the player (private message).
     * @param senderId The ID of the client requesting the view.
     * @param gameId The ID of the active game.
     * @return A private SHOWDOWN message containing the player's hand.
     */
    private ServiceResult handleShow(String senderId, String gameId) {
        GameEngine engine = getEngine(gameId);
        Player player = findPlayer(engine, senderId);

        // Hand of cards in format from helper method formatHandForPlayer
        String handString = formatHandForPlayer(player);

        // creating message SHOW and showing cards in hand
        String message = CommandParser.createMessage(CommandType.SHOW, "Player's ", player.getName(), " hand: ", handString);

        return new ServiceResult(message, false, gameId);
    }

    /**
     * A helper method to format a player's hand into a string.
     * @param player The player whose hand to format.
     * @return A formatted string of the player's cards.
     */
    // Helper method to format cards info to: 'RANK of SUIT', for example 'JACK of Spades'
    private String formatHandForPlayer(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Card card : player.getHand().getCards()) {
            sb.append(card.toString());
            sb.append(", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");

        return sb.toString().trim();
    }

    /**
     * A private method that handles betting and checking commands (BET, CALL, CHECK, FOLD).
     * @param senderId The ID of the client making the move.
     * @param gameId The ID of the active game.
     * @param cmd The command specifying the move and amount.
     * @return An ACTION broadcast message or an END message if the game concludes.
     */
    private ServiceResult handleMove(String senderId, String gameId, GameCommand cmd) {
        GameEngine engine = activeGames.get(gameId);
        if (engine == null) throw new IllegalArgumentException("Game does not exist or you are not in it.");

        Player player = findPlayer(engine, senderId);

        String action = cmd.getType().name();
        // 0 if no params after
        int amount = cmd.getParameters().length > 0 ? cmd.getIntParam(0) : 0;

        // makes action in engine
        engine.handleBetMove(player, action, amount);

        if (engine.getState() == GameState.PAYOUT) {
            engine.handlePayout();
        }

        if (engine.getState() == GameState.END) {
            return handleEndGame(engine, gameId);
        }

        // if changing phase
        if (engine.getState() != GameState.BET1 && engine.getState() != GameState.BET2) {
            //instruction for new phase
            return broadcastPhaseInfo(engine);
        }
        String actionMsg;
        if (amount == 0) {
            actionMsg = CommandParser.createMessage(CommandType.ACTION, player.getName(), action);
        }
        else{
            actionMsg = CommandParser.createMessage(CommandType.ACTION, player.getName(), action, String.valueOf(amount));
        }


        Player nextPlayer = engine.getPlayers().get(engine.getPlayersTurnIdx());
        String turnInfo = "\n--> Player's turn: " + nextPlayer.getName() + ". Pool: " + engine.getCurrentBet();

        return new ServiceResult(actionMsg + turnInfo, true, gameId);
    }

    /**
     * A private method that handles the DRAW command. Processes card exchanges for the current player.
     * @param senderId The ID of the client making the draw.
     * @param gameId The ID of the active game.
     * @param cmd The DRAW command listing card indices to discard.
     * @return A DRAWOK broadcast message or an END message if the game concludes after the draw.
     */
    private ServiceResult handleDraw(String senderId, String gameId, GameCommand cmd) {
        GameEngine engine = activeGames.get(gameId);
        if (engine == null) throw new IllegalArgumentException("Game does not exist or you are not in it.");

        Player player = findPlayer(engine, senderId);

        List<Integer> cardsToDiscard = cmd.getIntListParams();

        engine.handleDraw(player, cardsToDiscard);

        // checking PAYOUT / END
        if (engine.getState() == GameState.PAYOUT) {
            engine.handlePayout(); // prize and changing to end
        }

        if (engine.getState() == GameState.END) {
            return handleEndGame(engine, gameId); // winner info
        }

        String drawMsg = CommandParser.createMessage(CommandType.DRAWOK, player.getName(), String.valueOf(cardsToDiscard.size()));

        // changing phase info
        if (engine.getState() == GameState.BET2) {
            // BET2 instruction
            return broadcastPhaseInfo(engine);
        }

        return new ServiceResult(drawMsg, true, gameId);
    }

    /**
     * A private method that finalizes the game state transition from PAYOUT to END, generating the final outcome messages.
     * @param engine The GameEngine that has concluded the round.
     * @param gameId The ID of the game.
     * @return A broadcast ServiceResult containing the WINNER and END messages.
     */
    public ServiceResult handleEndGame(GameEngine engine, String gameId) {
        // generates command about the winner and results
        Player winner = engine.getRoundWinner();
        String winnerMsg = CommandParser.createMessage(CommandType.WINNER,
                winner.getName()
        );
        String endMsg = CommandParser.createMessage(CommandType.END, "OF THE ROUND");

        // as on broadcast
        String broadcast = winnerMsg + endMsg;

        return new ServiceResult(broadcast, true, gameId);
    }

    /**
     * A private method that handles the STATUS command. Generates a private report on the current game state
     * to the player that requested stats.
     * @param senderId The ID of the client requesting the status.
     * @param gameId The ID of the active game.
     * @return A private STATUS message containing phase, chips, pool, and turn information.
     */
    private ServiceResult handleStatus(String senderId, String gameId) {
        GameEngine engine = activeGames.get(gameId);
        if (engine == null) return createErrorResult("You are not in the game");

        //  finding a player that requested STATUS command
        Player requestingPlayer = findPlayer(engine, senderId);

        // finding an information about a player whose turn it is now for info in STATUS
        Player turnPlayer = engine.getPlayers().get(engine.getPlayersTurnIdx());

        String statusInfo = String.format("""
                --------------------
                Phase: %s
                Your name: %s
                Your chips: %d
                Current turn: %s
                Current pool: %d
                Current bet: %d
                Game ID: %s
                --------------------
                """,
                engine.getState().name(),
                requestingPlayer.getName(), // player's name who requested STATUS
                requestingPlayer.getChips(), // player's chips amount who requested STATUS
                turnPlayer.getName(), // whose turn it is
                engine.getPool(),
                engine.getCurrentBet(),
                engine.getGameId());

        return new ServiceResult(
                CommandParser.createMessage(CommandType.STATUS, statusInfo),
                false, gameId
        );
    }

    /**
     * A private method that handles the HELP command. Generates an instruction text for the client.
     * @param gameId The ID of the active game.
     * @return A private message containing the full instruction set.
     */
    private ServiceResult handleHelp(String gameId) {
        // using StringBuilder because it is more stable
        StringBuilder helpText = new StringBuilder();
        helpText.append(" -=-=-=-=-=-=-=-=-=-=-=-=- INSTRUCTION HOW TO USE POKER-SERVER -=-=-=-=-=-=-=-=-=-=-=-=- ").append("\n");
        helpText.append("Every command should be written upper case.").append("\n");
        helpText.append("\n");

        helpText.append("--- MANAGING SESSION AND GAME ---").append("\n");
        helpText.append("1. HELLO 1.0        : Starts a session with server.").append("\n");
        helpText.append("2. CREATE <ante> <bet>: Creates new table, example: CREATE 20 50").append("\n");
        helpText.append("3. JOIN <gameId> <name>: Joins existing game. example: JOIN game-abc Krzys").append("\n");
        helpText.append("4. START            : RStarts the game (only for host, minimum of 2 players).").append("\n");
        helpText.append("5. STATUS           : Shows round info, your chips and whose turn it is.").append("\n");
        helpText.append("6. SHOW             : Shows your current cards in hand.").append("\n");
        helpText.append("7. QUIT             : Ends a session and disconnects a client.").append("\n");

        helpText.append("\n--- ACTIONS DURING THE BETTING  ---").append("\n");
        helpText.append("8. BET <amount>     : Raises the stake or equalizes. The amount must be > 0.").append("\n");
        helpText.append("9. CALL             : Equalizes to the current highest rate.").append("\n");
        helpText.append("10. CHECK           : Checks (if no one has raised).").append("\n");
        helpText.append("11. FOLD            : Folds and gives up the round.").append("\n");

        helpText.append("\n--- ACTIONS DURING DRAWING ---").append("\n");
        helpText.append("12. DRAW <i j k>    : Exchanges cards with the given indexes (0-4). Max 5 cards.").append("\n");
        helpText.append("Example (exchange 1st, 2nd and 4th cards): DRAW 0 1 3").append("\n");
        helpText.append("\n------------------------------------------------------------------------------------");

        // returning MSG and instruction as one StringBuilder
        return new ServiceResult(
                CommandParser.createMessage(CommandType.MSG, helpText.toString()),
                false,
                gameId
        );
    }

    /**
     * A helper method to generate and return a broadcast message with instructions for the current phase.
     * @param engine The current game engine state.
     * @return A ServiceResult containing the broadcast message (INFO/MSG).
     */
    private ServiceResult broadcastPhaseInfo(GameEngine engine) {
        String phaseName = engine.getState().name();
        String message = "\n-=-=-=-=-=-=-=-=-=-=-=-=- PHASE: " + phaseName + " -=-=-=-=-=-=-=-=-=-=-=-=-";
        String turnPlayerName = engine.getPlayers().get(engine.getPlayersTurnIdx()).getName();

        if (engine.getState() == GameState.BET1 || engine.getState() == GameState.BET2) {
            message += "\nThis Phase is for betting. Player's turn: " + turnPlayerName + ".";
            message += "\nAllowed commands: BET <bet>, CALL, CHECK, FOLD, SHOW (showing cards in player's hand).";
            message += "\n(Actual bet to equalize: " + engine.getCurrentBet() + " chips.)";
        } else if (engine.getState() == GameState.DRAW) {
            message += "\nThis Phase is for drawing cards. Player's turn: " + turnPlayerName + ".";
            message += "\nAllowed commands: DRAW <indexesOfCards> (e.g. DRAW 0 2 4) (indexes from 0 to 4!), SHOW (showing cards in player's hand).";
        } else if (engine.getState() == GameState.END) {
            message += "\nThe round has ended. Use command CREATE or JOIN, to start new round .";
        } else if (engine.getState() == GameState.LOBBY) {
            message += "\nWaiting for all the players. Use command START, when everybody is ready.";
        }

        message = message + "\n-------------------------------------------------------------------------";

        return new ServiceResult(
                CommandParser.createMessage(CommandType.MSG, message),
                true, // broadcast message for everyone
                engine.getGameId()
        );
    }

    /**
     * A method that handles the QUIT command. Removes the player from the current game and
     * removes their mapping from playerGameMap. Sends a broadcast notification
     * to remaining players.
     * @param senderId The ID of the client issuing the QUIT command.
     * @param gameId The ID of the game being left.
     * @return A ServiceResult containing a private OK message for the quitter and a broadcast
     * LOBBY message for the remaining players.
     */
    private ServiceResult handleQuit(String senderId, String gameId) {
        if (gameId != null && activeGames.containsKey(gameId)) {
            GameEngine engine = activeGames.get(gameId);

            // find and remove the player from game engine
            Player playerToRemove = findPlayer(engine, senderId);
            String playerName = playerToRemove.getName();

            engine.getPlayers().remove(playerToRemove);

            if (engine.getPlayers().isEmpty()) {
                activeGames.remove(gameId);
            }

            // remove player from the map
            playerGameMap.remove(senderId);

            // other players get a notification that another player left
            if (activeGames.containsKey(gameId)) {
                return new ServiceResult(
                        CommandParser.createMessage(CommandType.OK, playerName + " left the game. Active players: " + getPlayerNames(engine)),
                        true,
                        gameId
                );
            }
        }

        playerGameMap.remove(senderId);
        return new ServiceResult(
                CommandParser.createMessage(CommandType.OK, "Leaving current game..."),
                true,
                null
        );
    }

    // -=-=-=-=-=- Helper methods -=-=-=-=-=-

    /**
     * A private method that searches for a Player object within the current game based on their network ID.
     * @param engine The game engine to search within.
     * @param senderId The unique network ID of the player.
     * @return The found Player object.
     * @throws IllegalStateException if the player is not found in the game.
     */
    private Player findPlayer(GameEngine engine, String senderId) {
        for (Player p : engine.getPlayers()) {
            if (p.getId().equals(senderId)) {
                return p;
            }
        }
        throw new IllegalStateException("Player " + senderId + " not found.");
    }

    /**
     * A private method that generates a string of all player names in the game, separated by spaces.
     * @param engine The game engine containing the player list.
     * @return A space-separated list of player names.
     */
    private String getPlayerNames(GameEngine engine) {
        // Converts players list to the string with spaces
        StringBuilder sb = new StringBuilder();
        for (Player p : engine.getPlayers()) {
            sb.append(p.getName()).append(" ");
        }
        return sb.toString().trim();
    }

    /**
     * A private method that creates a ServiceResult containing an error message based on an error code.
     * @param code The protocol error code (for example INVALID_ARG).
     * @param message The detailed error message.
     * @return A private error ServiceResult.
     */
    private ServiceResult createErrorResult(String code, String message) {
        return new ServiceResult(
                CommandParser.createMessage(CommandType.ERR, code, message),
                false, null
        );
    }

    /**
     * A private simplified method that creates an error result.
     * @param message The detailed error message.
     * @return A private error ServiceResult.
     */
    private ServiceResult createErrorResult(String message) {
        return createErrorResult("Error", message);
    }

    /**
     * A public getter that gets a specific GameEngine instance by its ID.
     * It is used by the network layer to read game state.
     * @param gameId The ID of the game to retrieve.
     * @return The GameEngine instance.
     * @throws IllegalArgumentException if the game does not exist.
     */
    public GameEngine getEngine(String gameId) {
        GameEngine engine = activeGames.get(gameId);
        if (engine == null) {
            throw new IllegalArgumentException("Game with ID " + gameId + " does not exist.");
        }
        return engine;
    }
}
