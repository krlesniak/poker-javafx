package game;

import cards.Deck;
import hierarchy.HandChecker;
import hierarchy.HandValue;
import players.Player;

import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    private final String gameId;
    private final Deck deck;
    private final List<Player> players;
    private final HandChecker handChecker;
    private final int minBet;
    private final int anteAmount;

    private GameState state;
    private int pool;
    private int currentBet;
    private int playersTurnIdx;
    private Player roundWinner;
    private String winningDesc = "";

    public GameEngine(String gameId, int anteAmount, int minBet) {
        this.gameId = gameId;
        this.deck = new Deck();
        this.players = new ArrayList<>();
        this.state = GameState.LOBBY;
        this.pool = 0;
        this.currentBet = 0;
        this.anteAmount = anteAmount;
        this.handChecker = new HandChecker();
        this.minBet = minBet;
    }

    public void addPlayer(Player player) {
        if (state != GameState.LOBBY && state != GameState.END) throw new IllegalStateException("Gra trwa");
        if (players.stream().noneMatch(p -> p.getId().equals(player.getId()))) {
            players.add(player);
        }
    }

    public void restartGame() {
        if (players.size() < 2) throw new IllegalStateException("Za mało graczy");

        this.state = GameState.ANTE;
        this.pool = 0;
        this.currentBet = 0;
        this.roundWinner = null;
        this.winningDesc = "";

        for (Player p : players) {
            p.resetRound();
            if (p.getChips() >= anteAmount) {
                p.bet(anteAmount);
                pool += anteAmount;
            } else {
                p.fold();
            }
        }

        // Resetujemy zakłady po ante
        players.forEach(Player::newPhaseReset);

        this.state = GameState.DEAL;
        deck.reset();
        for (int i = 0; i < 5; i++) {
            for (Player p : players) {
                if(!p.isFolded()) deck.deal().ifPresent(c -> p.getHand().addCard(c));
            }
        }

        this.state = GameState.BET1;
        this.currentBet = 0;
        this.playersTurnIdx = 0;
        while (players.get(playersTurnIdx).isFolded()) {
            playersTurnIdx = (playersTurnIdx + 1) % players.size();
        }
    }

    public void startGame() {
        restartGame();
    }

    public void handleBetMove(Player player, String action, int amount) {
        if (players.indexOf(player) != playersTurnIdx) throw new IllegalStateException("Nie Twoja tura");

        switch (action.toUpperCase()) {
            case "FOLD" -> {
                player.fold();
                player.setActed(true);
            }
            case "CHECK" -> {
                if (currentBet > player.getCurrBet()) throw new IllegalArgumentException("Musisz wyrównać (CALL) lub przebić (BET)");
                player.setActed(true);
            }
            case "CALL" -> {
                int toCall = currentBet - player.getCurrBet();
                if (toCall > 0) {
                    player.bet(toCall);
                    pool += toCall;
                }
                player.setActed(true);
            }
            case "BET" -> {
                if (amount < minBet) throw new IllegalArgumentException("Za mały BET");
                int potentialTotal = player.getCurrBet() + amount;
                if (potentialTotal <= currentBet) throw new IllegalArgumentException("Przebicie musi być wyższe");

                player.bet(amount);
                pool += amount;
                currentBet = player.getCurrBet();

                for(Player p : players) {
                    if (p != player && !p.isFolded()) p.setActed(false);
                }
                player.setActed(true);
            }
        }
        nextTurn();
    }

    public void handleDraw(Player player, List<Integer> indices) {
        if (state != GameState.DRAW) throw new IllegalStateException("To nie DRAW");
        if (players.indexOf(player) != playersTurnIdx) throw new IllegalStateException("Nie Twoja tura");

        player.getHand().removeCards(indices);
        for (int i = 0; i < indices.size(); i++) deck.deal().ifPresent(c -> player.getHand().addCard(c));

        player.setActed(true);
        nextTurn();
    }

    private void nextTurn() {
        if (players.stream().filter(p -> !p.isFolded()).count() <= 1) {
            doShowdown();
            return;
        }
        if (isPhaseFinished()) {
            changeState();
            return;
        }
        do {
            playersTurnIdx = (playersTurnIdx + 1) % players.size();
        } while (players.get(playersTurnIdx).isFolded());
    }

    private boolean isPhaseFinished() {
        return players.stream().filter(p -> !p.isFolded())
                .allMatch(p -> p.hasActed() && (state == GameState.DRAW || p.getCurrBet() == currentBet));
    }

    private void changeState() {
        players.forEach(Player::newPhaseReset);
        currentBet = 0;
        playersTurnIdx = 0;
        while (players.get(playersTurnIdx).isFolded()) playersTurnIdx++;

        if (state == GameState.BET1) state = GameState.DRAW;
        else if (state == GameState.DRAW) state = GameState.BET2;
        else if (state == GameState.BET2) doShowdown();
    }

    private void doShowdown() {
        state = GameState.SHOWDOWN;
        Player winner = null; HandValue best = null;
        for (Player p : players) {
            if (!p.isFolded()) {
                HandValue v = handChecker.checker(p.getHand());
                if (winner == null || v.compareTo(best) > 0) { winner = p; best = v; }
            }
        }
        roundWinner = winner;
        // ZMIANA: Teraz pobieramy tylko nazwę rankingu, bez brzydkich nawiasów []
        winningDesc = (best != null) ? best.ranking().toString() : "Walkower";
        state = GameState.PAYOUT;
    }

    public void handlePayout() {
        if (roundWinner != null) roundWinner.addChips(pool);
        state = GameState.END;
    }

    public int getAnteAmount() { return anteAmount; }
    public GameState getState() { return state; }
    public int getPool() { return pool; }
    public int getCurrentBet() { return currentBet; }
    public List<Player> getPlayers() { return players; }
    public Player getRoundWinner() { return roundWinner; }
    public String getWinningDesc() { return winningDesc; }
    public int getPlayersTurnIdx() { return playersTurnIdx; }
}