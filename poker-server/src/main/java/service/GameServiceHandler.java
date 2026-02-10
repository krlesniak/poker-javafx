package service;

import cards.Card;
import common.*;
import game.GameEngine;
import game.GameState;
import hierarchy.HandChecker;
import players.Player;
import java.util.*;

public class GameServiceHandler {
    private final Map<String, GameEngine> activeGames = Collections.synchronizedMap(new HashMap<>());
    public final Map<String, String> playerGameMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> gameHosts = Collections.synchronizedMap(new HashMap<>());
    private final HandChecker handChecker = new HandChecker();

    public record ServiceResult(String message, boolean broadcast, String targetGameId) {}

    public ServiceResult processCommand(String senderId, String currentGameId, GameCommand cmd) {
        String gameId = (currentGameId != null) ? currentGameId : playerGameMap.get(senderId);
        try {
            return switch (cmd.getType()) {
                case JOIN -> handleJoin(senderId, cmd);
                case START -> handleStart(senderId, gameId);
                case RESTART -> handleRestart(senderId, gameId);
                case FULL_RESET -> handleFullReset(senderId, gameId);
                case SHOWDOWN -> handleShowdownRequest(senderId, gameId);
                case BET, CALL, CHECK, FOLD -> handleMove(senderId, gameId, cmd);
                case DRAW -> handleDraw(senderId, gameId, cmd);
                default -> new ServiceResult(CommandParser.createMessage(CommandType.OK), false, gameId);
            };
        } catch (Exception e) {
            return new ServiceResult(CommandParser.createMessage(CommandType.ERR, e.getMessage()), false, null);
        }
    }

    private ServiceResult handleJoin(String senderId, GameCommand cmd) {
        String requestedGameId = cmd.getParameters()[0];
        String finalGameId = requestedGameId;

        // logic for a new table
        GameEngine existingEngine = activeGames.get(requestedGameId);

        // if a table is full we create a new one
        if (existingEngine != null && existingEngine.getPlayers().size() >= 4) {
            int tableNumber = 2;
            while (true) {
                String candidateId = requestedGameId + "_" + tableNumber;
                GameEngine nextTable = activeGames.get(candidateId);
                if (nextTable == null || nextTable.getPlayers().size() < 4) {
                    finalGameId = candidateId;
                    break;
                }
                tableNumber++;
            }
        }

        // loading a table
        GameEngine engine = activeGames.computeIfAbsent(finalGameId, id -> {
            gameHosts.put(id, senderId);
            return new GameEngine(id, 10, 10);
        });

        // adding a player
        Player existing = engine.getPlayers().stream().filter(p -> p.getId().equals(senderId)).findFirst().orElse(null);
        Player p = (existing != null) ? existing : new Player(senderId, "Player_" + (engine.getPlayers().size() + 1), 1000);

        if (existing == null) engine.addPlayer(p);
        playerGameMap.put(senderId, finalGameId);

        String hostId = gameHosts.get(finalGameId);
        StringBuilder sb = new StringBuilder();

        sb.append(CommandParser.createMessage(CommandType.WELCOME, finalGameId, senderId, hostId));

        for (Player pl : engine.getPlayers()) {
            sb.append(CommandParser.createMessage(CommandType.JOIN, pl.getId(), pl.getName(), String.valueOf(pl.getChips())));
        }

        if (engine.getDealerIdx() != -1) {
            sb.append(CommandParser.createMessage(CommandType.DEALER, engine.getPlayers().get(engine.getDealerIdx()).getId()));
        }

        return new ServiceResult(sb.toString(), true, finalGameId);
    }

    private ServiceResult handleStart(String senderId, String gameId) {
        if (!senderId.equals(gameHosts.get(gameId))) return new ServiceResult(CommandParser.createMessage(CommandType.ERR, "Only Host can start"), false, null);
        return handleRestart(senderId, gameId);
    }

    private ServiceResult handleRestart(String senderId, String gameId) {
        GameEngine engine = getEngine(gameId);
        try { engine.restartGame(); } catch (Exception e) { return new ServiceResult(CommandParser.createMessage(CommandType.ERR, e.getMessage()), false, null); }
        return new ServiceResult(generateSyncMessages(engine), true, gameId);
    }

    private ServiceResult handleFullReset(String senderId, String gameId) {
        if (!senderId.equals(gameHosts.get(gameId))) return new ServiceResult(CommandParser.createMessage(CommandType.ERR, "Only Host can reset"), false, null);
        GameEngine engine = getEngine(gameId);
        engine.hardResetGame();
        return new ServiceResult(CommandParser.createMessage(CommandType.LOG, "GAME_HAS_BEEN_RESET") + generateSyncMessages(engine), true, gameId);
    }

    private ServiceResult handleShowdownRequest(String senderId, String gameId) {
        GameEngine engine = getEngine(gameId);
        if (engine.getDealerIdx() == -1) return null;
        if (!senderId.equals(engine.getPlayers().get(engine.getDealerIdx()).getId())) return null;

        engine.processShowdown();
        StringBuilder sb = new StringBuilder();
        for (Player p : engine.getPlayers()) {
            if (!p.isFolded()) sb.append("REVEAL ").append(p.getId()).append(" ").append(getCardsString(p)).append("\n");
        }
        Player winner = engine.getRoundWinner();
        engine.handlePayout();
        sb.append(CommandParser.createMessage(CommandType.WINNER, winner.getName(), String.valueOf(engine.getPool()), engine.getWinningDesc().replace(" ", "_")));
        sb.append(CommandParser.createMessage(CommandType.LOG, "WINNER:_" + winner.getName() + "_(" + engine.getWinningDesc().replace(" ", "_") + ")"));
        sb.append(CommandParser.createMessage(CommandType.JOIN, winner.getId(), winner.getName(), String.valueOf(winner.getChips())));
        return new ServiceResult(sb.toString(), true, gameId);
    }

    private String generateSyncMessages(GameEngine engine) {
        StringBuilder sb = new StringBuilder();
        sb.append(CommandParser.createMessage(CommandType.STARTED, String.valueOf(engine.getAnteAmount())));
        sb.append(CommandParser.createMessage(CommandType.ROUND, String.valueOf(engine.getPool()), "0"));
        for (Player p : engine.getPlayers()) {
            sb.append(CommandParser.createMessage(CommandType.JOIN, p.getId(), p.getName(), String.valueOf(p.getChips())));
        }
        if (engine.getDealerIdx() != -1) {
            String dealerId = engine.getPlayers().get(engine.getDealerIdx()).getId();
            sb.append(CommandParser.createMessage(CommandType.DEALER, dealerId));
        }

        // dealer starts
        Player active = engine.getPlayers().get(engine.getPlayersTurnIdx());
        sb.append(CommandParser.createMessage(CommandType.TURN, active.getId(), active.getName(), engine.getState().name(), String.valueOf(engine.getCurrentBet())));

        return sb.toString();
    }

    private ServiceResult handleMove(String senderId, String gameId, GameCommand cmd) {
        GameEngine engine = getEngine(gameId);
        Player p = engine.getPlayers().stream().filter(pl -> pl.getId().equals(senderId)).findFirst().orElseThrow();
        int amount = cmd.getParameters().length > 0 ? cmd.getIntParam(0) : 0;
        engine.handleBetMove(p, cmd.getType().name(), amount);

        String base = CommandParser.createMessage(CommandType.ACTION, senderId, cmd.getType().name(), String.valueOf(p.getTotalRoundBet()), "0") +
                CommandParser.createMessage(CommandType.ROUND, String.valueOf(engine.getPool()), String.valueOf(engine.getCurrentBet())) +
                CommandParser.createMessage(CommandType.JOIN, p.getId(), p.getName(), String.valueOf(p.getChips())) +
                CommandParser.createMessage(CommandType.LOG, p.getName() + "_" + cmd.getType().name());

        // last player standing
        if (engine.getState() == GameState.PAYOUT) {
            Player winner = engine.getRoundWinner();
            int finalPool = engine.getPool();
            String desc = engine.getWinningDesc();
            engine.handlePayout();

            String winMsg = CommandParser.createMessage(CommandType.WINNER, winner.getName(), String.valueOf(finalPool), desc);
            String logWin = CommandParser.createMessage(CommandType.LOG, "WINNER:_" + winner.getName() + "_by_walkover");
            String chipsUpdate = CommandParser.createMessage(CommandType.JOIN, winner.getId(), winner.getName(), String.valueOf(winner.getChips()));

            return new ServiceResult(base + winMsg + logWin + chipsUpdate, true, gameId);
        }

        if (engine.getState() == GameState.SHOWDOWN) {
            String dealerId = engine.getPlayers().get(engine.getDealerIdx()).getId();
            return new ServiceResult(base + CommandParser.createMessage(CommandType.LOG, "WAITING_FOR_SHOWDOWN") + "READY_SHOWDOWN " + dealerId, true, gameId);
        }

        // notification about next round
        Player active = engine.getPlayers().get(engine.getPlayersTurnIdx());
        String turnMsg = CommandParser.createMessage(CommandType.TURN, active.getId(), active.getName(), engine.getState().name(), String.valueOf(engine.getCurrentBet()));

        return new ServiceResult(base + turnMsg, true, gameId);
    }

    private ServiceResult handleDraw(String senderId, String gameId, GameCommand cmd) {
        GameEngine engine = getEngine(gameId);
        Player p = engine.getPlayers().stream().filter(pl -> pl.getId().equals(senderId)).findFirst().orElseThrow();
        engine.handleDraw(p, cmd.getIntListParams());
        String handStr = getCardsString(p);
        String rankStr = handChecker.checker(p.getHand()).ranking().toString().replace(" ", "_");

        String drawResult = CommandParser.createMessage(CommandType.DEAL, handStr + " STR:" + rankStr) + CommandParser.createMessage(CommandType.LOG, p.getName() + "_EXCHANGED");

        Player active = engine.getPlayers().get(engine.getPlayersTurnIdx());
        String turnMsg = CommandParser.createMessage(CommandType.TURN, active.getId(), active.getName(), engine.getState().name(), String.valueOf(engine.getCurrentBet()));

        return new ServiceResult(drawResult + turnMsg, false, gameId);
    }

    private String getCardsString(Player p) {
        StringBuilder sb = new StringBuilder();
        for (Card c : p.getHand().getCards()) sb.append(c.rank().name()).append("_").append(c.suit().name()).append(" ");
        return sb.toString().trim();
    }

    public GameEngine getEngine(String gameId) {
        GameEngine e = activeGames.get(gameId);
        if (e == null) throw new IllegalArgumentException("No Game found for ID: " + gameId);
        return e;
    }
}