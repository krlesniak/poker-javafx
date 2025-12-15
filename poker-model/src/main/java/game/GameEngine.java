package game;

import java.util.ArrayList;
import java.util.List;

import cards.Card;
import cards.Deck;
import exceptions.StateMismatchException;
import hierarchy.HandChecker;
import players.Player;
import java.util.Optional;
import exceptions.OutOfTurnException;
import exceptions.IllegalDrawException;
import hierarchy.HandValue;

/**
 * A class that is responsible for the correct round course and correct state change
 */
public class GameEngine {
    /**
     * A private final String if od the game
     */
    private final String gameId;
    /**
     * A private final deck of the ccards
     */
    private final Deck deck;
    /**
     * A private final list of the players
     */
    private final List<Player> players;
    /**
     * A private final hand checker that checks the layouts
     */
    private final HandChecker handChecker;

    /**
     * A private final int minimal amount of players' bet
     */
    private final int minBet;

    /**
     * A private game state of the round
     */
    private GameState state;
    /**
     * A private pool prize in the round
     */
    private int pool;
    /**
     * A private current bet in the round
     */
    private int currentBet;
    /**
     * A private ante amount that is given by every player every round
     */
    private int anteAmount;
    /**
     * A private variable that tells whose turn it is in the round
     */
    private int playersTurnIdx;

    /**
     * A private variable of the player that is the winner of a certain round
     */
    private Player roundWinner;

    /**
     * A constructor that sets every parameter
     * @param gameId A name of game id
     * @param anteAmount an amount of chips that will be given by evey player at the beginning of every round
     * @param minBet a minimal amount that players can bet during a round
     */
    public GameEngine(String gameId, int anteAmount, int minBet) {
        this.gameId = gameId;
        this.deck = new Deck();
        this.players = new ArrayList<>();
        this.state = GameState.LOBBY;
        this.pool = 0;
        this.currentBet = 0;
        this.anteAmount = anteAmount;
        this.handChecker = new HandChecker();
        this.roundWinner = null;
        this.minBet = minBet;
    }

    /**
     * A method that adds maximum of four players to the game
     * @param player that is added to the game
     */
    // adding players to the game
    public void addPlayer(Player player) {
        if (state != GameState.LOBBY) {
            throw new IllegalStateException("You cannot join the lobby during the game");
        }
        if (players.size() >= 4) {
            throw new IllegalStateException("Cannot start the game with more than 4 players");
        }
        if (players.contains(player)) {
            throw new IllegalStateException("This player already exists");
        }
        players.add(player);
    }

    /**
     * A method that s responsible for starting the game,
     * game cannot be started when there is less than two players in the lobby
     */
    // starting the first phases of the game (ante, deal, bet1)
    public void startGame() {
        if (players.size() < 2) {
            throw new IllegalStateException("Cannot start the game with less than 2 players");
        }

        // taking ante
        this.state = GameState.ANTE;
        takeAnte();

        // reseting player's current bet not to get a mistake in betting phase
        for(Player p : players) {
            p.newPhaseReset();
        }
        this.currentBet = 0; // resetting bet

        // dealing cards
        this.state = GameState.DEAL;
        dealCards();

        // beginning of licitation
        this.state = GameState.BET1;
        this.playersTurnIdx = 0;
    }

    /**
     * A method that handles DRAW state, when players can exchange up to five cards from their hands
     * @param p player in the game
     * @param cardToRemove how many cards (from 0 to 5) a player wants to draw
     */
    // state DRAW -> players exchange as many cards from their hands (<5)
    // and draw new cards from the deck
    public void handleDraw(Player p, List<Integer> cardToRemove) {
        correctTurn(p);
        if (state != GameState.DRAW) {
            throw new StateMismatchException(state, GameState.DRAW);
        }
        if (cardToRemove.size() > 5) {
            throw new IllegalDrawException("You cannot exchange more than five cards");
        }
        // remove cards from hand
        p.getHand().removeCards(cardToRemove);
        // new cards
        int cardsNeeded = cardToRemove.size();

        // taking new cards from the deck
        for (int i = 0; i < cardsNeeded; i++) {
            // safe, because we prevent a situation when there is no card to take (null)
            Optional<Card> cardOpt = deck.deal();
            if (cardOpt.isPresent()) {
                Card newCard = cardOpt.get();
                p.getHand().addCard(newCard);
            } else {
                throw new IllegalDrawException("No cards in Deck");
            }
        }

        nextTurn();
    }

    /**
     * A method that handles bet state in the round
     * @param player that is active in the round and bets / calls / checks / folds
     * @param action possible actions in this state : BET / FOLD / CALL / CHECK
     * @param amount of chips that a player give to the pool during the round
     */
    // BETTING state -> player can either fold or bet
    public void handleBetMove(Player player, String action, int amount) {

        // Check if game state is correct
        if (state != GameState.BET1 && state != GameState.BET2) {
            throw new StateMismatchException(state, GameState.BET1);
        }
        // Check if it's player's turn
        correctTurn(player);

        switch (action.toUpperCase()) {
            case "FOLD":
                handleFold(player); // Delegates logic to private method
                break;
            case "BET":
                handleBet(player, amount);
                break;
            case "CALL":
                handleCall(player);
                break;
            case "CHECK":
                handleCheck(player);
                break;
            default:
                throw new IllegalArgumentException("Unknown action: " + action);
        }
        nextTurn();
    }

    /**
     * A private method that handles FOLD action
     * @param player whose turn it is
     */
    // A private method that handles FOLD action
    private void handleFold(Player player) {
        player.fold();
        player.setActed(true);
    }

    /**
     * A private method that handles BET action
     * @param player whose turn it is
     * @param amount how much o bet
     */
    // A private method that handles BET action
    private void handleBet(Player player, int amount) {
        // if a player tries to bet less then minimum bet
        if (amount < minBet) {
            throw new IllegalArgumentException("Bet too small! Minimum is: " + minBet);
        }

        // validation of other players betting
        if (player.getCurrBet() + amount < this.currentBet) {
            throw new IllegalArgumentException("Not enough to match. Current bet is: " + currentBet);
        }
        player.bet(amount);
        this.pool += amount;

        if (player.getCurrBet() > this.currentBet) {
            this.currentBet = player.getCurrBet();
        }
        player.setActed(true);
    }

    /**
     * A private method that handles CALL action
     * @param player whose turn it is now
     */
    // A private method that handles CALL action
    private void handleCall(Player player) {
        int toCall = this.currentBet - player.getCurrBet();

        if (toCall > 0) {
            player.bet(toCall);
            this.pool += toCall;
        }
        // CALL equalizes the curr bet
        player.setActed(true);
    }

    /**
     * A private method that handles CHECK action
     * @param player whose turn it is now
     */
    // A private method that handles CHECK action
    private void handleCheck(Player player) {
        // CHECK is possible only after equalizing
        if (player.getCurrBet() < this.currentBet) {
            throw new IllegalArgumentException("Cannot CHECK. You must CALL " + (this.currentBet - player.getCurrBet()));
        }
        player.setActed(true);
    }

    /**
     * A method that handles the payout for the winner of the round during PAYOUT phase
     */
    // giving the winning pool to the winner of the round
    public void handlePayout() {
        if (this.state != GameState.PAYOUT) {
            throw new StateMismatchException(state, GameState.PAYOUT);
        }

        if (this.roundWinner != null) {
            System.out.println("WINNER: " + roundWinner.getName() + " wins: " + pool);
            this.roundWinner.addChips(this.pool);
            this.pool = 0;
        }

        // End of the round
        resetForNewRound();
        this.state = GameState.END;
    }

    // -=-=-=-=-=-=-=-=- helper methods -=-=-=-=-=-=-=-=-

    /**
     * A private method that is responsible for taking ante from the players at the beginning of every round
     */
    // taking ante at the beginning of the round
    private void takeAnte() {
        for (Player player : players) {
            player.bet(anteAmount);
            pool += anteAmount;
        }
    }

    /**
     * A private method that is responsible for giving five cards for every player at the beginning of the round
     */
    // giving cards from the deck
    private void dealCards() {
        deck.reset();
        final int CARDS_PER_PLAYER = 5;
        for (int i = 0; i < CARDS_PER_PLAYER; i++) {
            for (Player player : players) {
                Optional<Card> cardOpt = deck.deal();
                if (cardOpt.isPresent()) {
                    player.getHand().addCard(cardOpt.get());
                }
                else{
                    System.out.println("The deck is empty");
                    return;
                }
            }
        }
    }

    /**
     * A private method that checks if the queue of turns in round is correct
     * @param player whose turn it should be
     */
    // checking if the queue of turns in round is correct
    private void correctTurn(Player player) {
        Player current = players.get(playersTurnIdx);
        if (!current.getId().equals(player.getId())) {
            throw new OutOfTurnException();
        }
    }

    /**
     * A private method that checks if the betting phase is going as it should be going
     * @return true if everything is going great or false if something is wrong
     */
    // checking if the BETTING state is going correctly
    private boolean isBettingCorrect(){
        int activePlayers = 0;
        for (Player player : players) {
            if (!player.isFolded()){
                activePlayers++;
                if (player.getCurrBet() < this.currentBet) {
                    return false;
                }
                if (!player.hasActed()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * A private method that is responsible for changing game states correctly during the round
     * BET1 -> DRAW -> BET2 -> SHOWDOWN
     */
    // changing the round state to the next one according to GameState class
    private void changeState(){
        for (Player player : players) {
            player.newPhaseReset();
        }
        this.currentBet = 0;
        this.playersTurnIdx = 0;

        // looking for the first active player
        while (players.get(playersTurnIdx).isFolded()) {
            playersTurnIdx = (playersTurnIdx + 1) % players.size();
        }

        switch (this.state) {
            case BET1:
                this.state = GameState.DRAW;
                break;
            case DRAW:
                this.state = GameState.BET2;
                break;
            case BET2:
                doShowdown();
                break;
            default:
                break;
        }
    }

    /**
     * A method that is responsible for changing the game state to showdown and revealing the round winner
     */
    // choosing the winner of the round by comparing their layouts on hand
    private void doShowdown(){
        this.state = GameState.SHOWDOWN;
        System.out.println("\n -=-=-=-=-=-=-=-=- SHOWDOWN -=-=-=-=-=-=-=-=-");

        // finding active players
        List<Player> activePlayers = new ArrayList<>();
        for (Player p : players) {
            if (!p.isFolded()) {
                activePlayers.add(p);
            }
        }

        if (activePlayers.isEmpty()) return;

        // if only one active player
        if (activePlayers.size() == 1) {
            this.roundWinner = activePlayers.get(0);
            this.state = GameState.PAYOUT;
            return;
        }

        // finding the highest hand (max)
        Player winner = null;
        HandValue bestHandValue = null;

        for (Player p : activePlayers) {
            HandValue currentVal = handChecker.checker(p.getHand());
            System.out.println("Player " + p.getName() + " hand: " + currentVal);

            if (winner == null || currentVal.compareTo(bestHandValue) > 0) {
                winner = p;
                bestHandValue = currentVal;
            }
        }

        this.roundWinner = winner;
        this.state = GameState.PAYOUT;
    }

    /**
     * A method that is responsible for changing the turn for the next player
     */
    // checking if the betting ended
    private void nextTurn() {
        int activeCount = 0;
        for(Player p : players) {
            if (!p.isFolded()) activeCount++;
        }
        if (activeCount <= 1) {
            doShowdown();
            return;
        }
        // remembering the idx
        int previousIdx = playersTurnIdx;

        // changing turn
        playersTurnIdx = (playersTurnIdx + 1) % players.size();
        while (players.get(playersTurnIdx).isFolded()) {
            playersTurnIdx = (playersTurnIdx + 1) % players.size();
        }

        // phase change
        if (state == GameState.DRAW) {
            // when the index change from higher to lower (3 to 0)
            if (playersTurnIdx < previousIdx) {
                changeState();
            }
        } else {
            if (isBettingCorrect()) {
                changeState();
            }
        }
    }

    /**
     * A private method that resets players' stats: hand, fold, current bet
     */
    // reset stats (hand, fold, bet) for the new round
    private void resetForNewRound() {
        for (Player player : players) {
            player.resetRound();
        }
        this.currentBet = 0;
    }

    /**
     * getter
     * @return current state of the game
     */
    // getters
    public GameState getState() {return state;}

    /**
     * getter
     * @return the current pool (prize) at the center of the table in the round
     */
    public  int getPool() {return pool;}

    /**
     * getter
     * @return the current bet iun the round
     */
    public int getCurrentBet() {return currentBet;}

    /**
     * getter
     * @return players a list of player in the game
     */
    public List<Player> getPlayers() {return players;}

    /**
     * getter
     * @return winner of the round
     */
    public Player getRoundWinner() {return roundWinner;}

    /**
     * getter
     * @return anteAmount amount of chips that is being taken at the beginning of every round
     */
    public int getAnteAmount() {return anteAmount;}

    /**
     * getter
     * @return gameId of the certain game started by host using command CREATE [ante] [bet]
     */
    public String getGameId(){return gameId;}

    /**
     * getter
     * @return playersTurnIdx id of player whose turn it is
     */
    public int getPlayersTurnIdx() {return playersTurnIdx;}
}