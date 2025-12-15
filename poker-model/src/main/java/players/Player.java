package players;

import exceptions.NotEnoughChipsException;

/**
 * A class that represents a single player in the poker table
 */
public class Player {
    /**
     * a private final string id of the player
     */
    private final String id;
    /**
     * a private final string name of the p[layer
     */
    private final String name;
    /**
     * a private int amount odf chips on player's hand
     */
    private int chips;
    /**
     * a private hand of the player
     */
    private Hand hand;
    /**
     * a private boolean variable that says if player folded or not in the round
     */
    private boolean folded;
    /**
     * a private int current bet in the round
     */
    private int currBet;
    /**
     * a private boolean flag used in GameEngine to track if player made some action (BET, FOLD, CALL, CHECK) in a current phase
     */
    private boolean hasActed;

    /** A constructor that creates a new player
     * @param id of the new player
     * @param name of the new player
     * @param startChips amount of starting chips of new player
     */
    public Player(String id, String name, int startChips) {
        this.id = id;
        this.name = name;
        this.chips = startChips;
        hand = new Hand();
        folded = false;
        currBet = 0;
        this.hasActed = false;
    }

    /**
     * A method that takes chips from the player and adds it to the pool
     * @param amount of chips in the bet
     */
    public void bet(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("bet amount must be positive!");
        }
        if (amount > this.chips){
            throw new NotEnoughChipsException(amount, this.chips);
        }
        this.currBet += amount;
        this.chips -= amount;
    }

    /**
     * A method that adds chips to the player
     * @param amount of chips added to the player
     */
    public void addChips(int amount) {
        if (amount > 0) {
            this.chips += amount;
        }
    }

    /**
     * A method that sets folded to true if player folded in the round
     */
    public void fold() {
        this.folded = true;
    }

    /**
     * A method that is responsible for resetting players stats for the new round
     * it clears his bet, hand, if he folded and if he had made some action during the round
     */
    public void resetRound() {
        this.currBet = 0;
        this.hand = new Hand();
        this.folded = false;
        this.hasActed = false;
    }

    /**
     * A method that resets player's bet before entering a new phase of the round (DRAW -> BET2)
     * it clears current bet and if the player has acted
     */
    public void newPhaseReset() {
        this.currBet = 0;
        this.hasActed = false;

    }

    /**
     * getter
     * @return id of the player
     */
    // getters
    public String getId() {return id;}

    /**
     * getter
     * @return name of the player
     */
    public String getName() {return name;}

    /**
     * geter
     * @return amount of player's chips
     */
    public int getChips() {return chips;}

    /**
     * getter
     * @return player's hand
     */
    public Hand getHand() {return hand;}

    /**
     * getter
     * @return if the player folded in this round (true / false)
     */
    public boolean isFolded() {return folded;}

    /**
     * getter
     * @return current bet
     */
    public int getCurrBet() {return currBet;}

    /**
     * getter
     * @return if the player has made any action (true / false)
     */
    public boolean hasActed() { return hasActed; }

    /**
     * setter
     * @param acted is being set
     */
    public void setActed(boolean acted) { this.hasActed = acted; }

    /**
     * setter
     * @param chips are being set to the certain value
     */
    public void setChips(int chips) {this.chips = chips;}

    /**
     * setter
     * @param hand is being set
     */
    public void setHand(Hand hand) {this.hand = hand;}
}