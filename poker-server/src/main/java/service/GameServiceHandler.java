package service;

import cards.Card;
import common.*;
import game.GameEngine;
import game.GameState;
import players.Player;
import java.util.*;

public class GameServiceHandler {
    private final Map<String, GameEngine> activeGames = Collections.synchronizedMap(new HashMap<>());
    public final Map<String, String> playerGameMap = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> gameHosts = Collections.synchronizedMap(new HashMap<>());

    public record ServiceResult(String message, boolean broadcast, String targetGameId) {}

    public ServiceResult processCommand(String senderId, String currentGameId, GameCommand cmd) {
        String gameId = (currentGameId != null) ? currentGameId : playerGameMap.get(senderId);
        try {
            return switch (cmd.getType()) {
                case JOIN -> handleJoin(senderId, cmd);
                case START -> handleStart(senderId, gameId);
                case RESTART -> handleRestart(senderId, gameId);
                case BET, CALL, CHECK, FOLD -> handleMove(senderId, gameId, cmd);
                case DRAW -> handleDraw(senderId, gameId, cmd);
                default -> new ServiceResult(CommandParser.createMessage(CommandType.OK), false, gameId);
            };
        } catch (Exception e) {
            return new ServiceResult(CommandParser.createMessage(CommandType.ERR, e.getMessage()), false, null);
        }
    }

    private ServiceResult handleJoin(String senderId, GameCommand cmd) {
        String gameId = cmd.getParameters()[0];
        GameEngine engine = activeGames.computeIfAbsent(gameId, id -> {
            gameHosts.put(id, senderId);
            return new GameEngine(id, 10, 50);
        });

        Player existing = engine.getPlayers().stream().filter(p -> p.getId().equals(senderId)).findFirst().orElse(null);
        Player p = (existing != null) ? existing : new Player(senderId, cmd.getParameters()[1], 1000);
        if (existing == null) engine.addPlayer(p);

        playerGameMap.put(senderId, gameId);

        StringBuilder response = new StringBuilder();
        response.append(CommandParser.createMessage(CommandType.WELCOME, gameId, senderId));
        response.append(CommandParser.createMessage(CommandType.JOIN, senderId, p.getName(), String.valueOf(p.getChips())));

        for (Player oldPlayer : engine.getPlayers()) {
            if (!oldPlayer.getId().equals(senderId)) {
                response.append(CommandParser.createMessage(CommandType.JOIN, oldPlayer.getId(), oldPlayer.getName(), String.valueOf(oldPlayer.getChips())));
            }
        }

        return new ServiceResult(response.toString(), true, gameId);
    }

    private ServiceResult handleStart(String senderId, String gameId) {
        if (!senderId.equals(gameHosts.get(gameId))) return new ServiceResult(CommandParser.createMessage(CommandType.ERR, "Only Host"), false, null);

        GameEngine engine = getEngine(gameId);
        engine.startGame();

        // POPRAWKA: Wysyłamy STARTED oraz od razu aktualny stan puli (Ante)
        String started = CommandParser.createMessage(CommandType.STARTED, String.valueOf(engine.getAnteAmount()));
        String round = CommandParser.createMessage(CommandType.ROUND, String.valueOf(engine.getPool()), "0");
        return new ServiceResult(started + round, true, gameId);
    }

    private ServiceResult handleRestart(String senderId, String gameId) {
        if (!senderId.equals(gameHosts.get(gameId))) return new ServiceResult(CommandParser.createMessage(CommandType.ERR, "Only Host"), false, null);

        GameEngine engine = getEngine(gameId);
        engine.restartGame();

        StringBuilder updates = new StringBuilder();
        updates.append(CommandParser.createMessage(CommandType.STARTED, String.valueOf(engine.getAnteAmount())));

        // POPRAWKA: Natychmiastowa aktualizacja puli po restarcie
        updates.append(CommandParser.createMessage(CommandType.ROUND, String.valueOf(engine.getPool()), "0"));

        for(Player p : engine.getPlayers()) {
            updates.append(CommandParser.createMessage(CommandType.JOIN, p.getId(), p.getName(), String.valueOf(p.getChips())));
        }

        return new ServiceResult(updates.toString(), true, gameId);
    }

    private ServiceResult handleMove(String senderId, String gameId, GameCommand cmd) {
        GameEngine engine = getEngine(gameId);
        Player p = engine.getPlayers().stream().filter(pl -> pl.getId().equals(senderId)).findFirst().orElseThrow();
        engine.handleBetMove(p, cmd.getType().name(), cmd.getParameters().length > 0 ? cmd.getIntParam(0) : 0);

        if (engine.getState() == GameState.PAYOUT) {
            engine.handlePayout();
            Player winner = engine.getRoundWinner();
            String msg = CommandParser.createMessage(CommandType.WINNER, winner.getName(), String.valueOf(engine.getPool()), engine.getWinningDesc().replace(" ", "_"));
            return new ServiceResult(msg, true, gameId);
        }
        return new ServiceResult(CommandParser.createMessage(CommandType.ROUND, String.valueOf(engine.getPool()), String.valueOf(engine.getCurrentBet())), true, gameId);
    }

    private ServiceResult handleDraw(String senderId, String gameId, GameCommand cmd) {
        GameEngine engine = getEngine(gameId);
        Player p = engine.getPlayers().stream().filter(pl -> pl.getId().equals(senderId)).findFirst().orElseThrow();
        engine.handleDraw(p, cmd.getIntListParams());

        StringBuilder sb = new StringBuilder();
        for (Card c : p.getHand().getCards()) sb.append(c.rank().name()).append("_").append(c.suit().name()).append(" ");
        return new ServiceResult(CommandParser.createMessage(CommandType.DEAL, sb.toString().trim()), false, gameId);
    }

    public GameEngine getEngine(String gameId) {
        GameEngine e = activeGames.get(gameId);
        if (e == null) throw new IllegalArgumentException("Brak gry");
        return e;
    }
}