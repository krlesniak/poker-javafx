package game;

import cards.Deck;
import hierarchy.HandChecker;
import hierarchy.HandValue;
import players.Player;
import java.util.List;
import java.util.ArrayList;

public class GameEngine {
    private final String gameId;
    private final Deck deck;
    private final List<Player> players;
    private final HandChecker handChecker;
    private final int minBet;
    private final int anteAmount;
    private boolean showdownTriggered = false; // flag for dealer

    private GameState state;
    private int pool;
    private int currentBet;
    private int playersTurnIdx;
    private int dealerIdx = -1; // spy for dealer
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
        if (state != GameState.LOBBY && state != GameState.END) throw new IllegalStateException("Game already started");
        if (players.stream().noneMatch(p -> p.getId().equals(player.getId()))) {
            players.add(player);
        }
    }

    public void hardResetGame() {
        if (players.size() < 2) throw new IllegalStateException("Too few players");
        for (Player p : players) {
            p.resetRound();
            int diff = 1000 - p.getChips();
            p.addChips(diff);
        }
        startGame();
    }

    public void restartGame() {
        if (players.size() < 2) throw new IllegalStateException("Too few players");
        for (Player p : players) {
            if (p.getChips() <= 0) throw new IllegalStateException("Player " + p.getName() +
                    " lost all his / her chips. click RESET GAME to start a new game.");
        }
        startGame();
    }

    public void startGame() {
        this.state = GameState.BET1;
        this.deck.reset();
        this.pool = 0;
        this.currentBet = 0;
        this.winningDesc = "";
        this.roundWinner = null;

        // moving dealer chip every round
        if (!players.isEmpty()) {
            dealerIdx = (dealerIdx + 1) % players.size();
        }

        for (Player p : players) {
            p.resetRound();
            int ante = Math.min(p.getChips(), anteAmount);
            p.bet(ante);
            pool += ante;

            for (int i = 0; i < 5; i++) {
                p.getHand().addCard(deck.deal().orElseThrow());
            }
        }

        players.forEach(Player::newPhaseReset);

        // dealer starts every round
        playersTurnIdx = dealerIdx;
    }

    public void handleBetMove(Player player, String move, int amount) {
        if (!players.get(playersTurnIdx).equals(player)) throw new IllegalStateException("Not your turn");

        switch (move) {
            case "FOLD" -> player.fold();
            case "CHECK" -> {
                if (player.getCurrBet() < currentBet) throw new IllegalArgumentException("You have to equalize (CALL)!");
                player.setActed(true);
            }
            case "CALL" -> {
                int needed = currentBet - player.getCurrBet();
                int toPay = Math.max(0, Math.min(needed, player.getChips()));
                player.bet(toPay);
                pool += toPay;
                player.setActed(true);
            }
            case "BET" -> {
                if (amount < minBet) throw new IllegalArgumentException("Minimal bet is" + minBet);
                if (amount > player.getChips()) throw new IllegalArgumentException("Insufficient funds!");

                int potentialTotal = player.getCurrBet() + amount;
                if (potentialTotal < currentBet) throw new IllegalArgumentException("The bet must match the stake");

                player.bet(amount);
                pool += amount;

                if (player.getCurrBet() > currentBet) {
                    currentBet = player.getCurrBet();
                    for(Player p : players) {
                        if (p != player && !p.isFolded() && p.getChips() > 0) p.setActed(false);
                    }
                }
                player.setActed(true);
            }
        }
        nextTurn();
    }

    public void handleDraw(Player player, List<Integer> indices) {
        if (state != GameState.DRAW) throw new IllegalStateException("Wrong phase");
        if (!players.get(playersTurnIdx).equals(player)) throw new IllegalStateException("It is not your turn");

        for (int i : indices) {
            if (i >= 0 && i < 5) {
                player.getHand().replace(i, deck.deal().orElseThrow());
            }
        }
        player.setActed(true);
        nextTurn();
    }

    private void nextTurn() {
        int active = (int) players.stream().filter(p -> !p.isFolded()).count();
        // last player standing -> winning by walkover
        if (active <= 1) {
            roundWinner = players.stream().filter(p -> !p.isFolded()).findFirst().orElse(null);
            winningDesc = "Other_Players_Folded";
            state = GameState.PAYOUT;
            return;
        }

        boolean phaseDone = players.stream()
                .filter(p -> !p.isFolded() && p.getChips() > 0)
                .allMatch(Player::hasActed);

        if (phaseDone) changeState();
        else {
            do {
                playersTurnIdx = (playersTurnIdx + 1) % players.size();
            } while (players.get(playersTurnIdx).isFolded() || players.get(playersTurnIdx).getChips() == 0);
        }
    }

    private void changeState() {
        players.forEach(Player::newPhaseReset);
        currentBet = 0;

        // looking for dealer in each phase
        playersTurnIdx = dealerIdx;
        while (players.get(playersTurnIdx).isFolded() || players.get(playersTurnIdx).getChips() == 0) {
            playersTurnIdx = (playersTurnIdx + 1) % players.size();
        }

        if (state == GameState.BET1) state = GameState.DRAW;
        else if (state == GameState.DRAW) state = GameState.BET2;
        else if (state == GameState.BET2) {
            state = GameState.SHOWDOWN;
        }
    }

    private void doShowdown() {
        Player winner = null; HandValue best = null;
        for (Player p : players) {
            if (!p.isFolded()) {
                HandValue v = handChecker.checker(p.getHand());
                if (winner == null || v.compareTo(best) > 0) { winner = p; best = v; }
            }
        }
        roundWinner = winner;
        winningDesc = (best != null) ? best.ranking().toString() : "Folded";
        state = GameState.PAYOUT;
    }

    public void processShowdown() {
        if (state != GameState.SHOWDOWN) return;
        doShowdown();
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
    public int getDealerIdx() { return dealerIdx; }
}