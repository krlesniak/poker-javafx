package players;

import players.Hand;

public class Player {
    private final String id;
    private final String name;
    private int chips;
    private int currBet; // Wkład w obecnej fazie
    private int totalRoundBet; // Suma z całego rozdania (kartoteka)
    private Hand hand;
    private boolean folded;
    private boolean acted;

    public Player(String id, String name, int chips) {
        this.id = id;
        this.name = name;
        this.chips = chips;
        this.hand = new Hand();
        this.resetRound();
    }

    public void resetRound() {
        this.folded = false;
        this.currBet = 0;
        this.totalRoundBet = 0;
        this.acted = false;
        this.hand.clear();
    }

    public void newPhaseReset() {
        this.currBet = 0;
        this.acted = false;
    }

    public void bet(int amount) {
        if (amount > chips) amount = chips;
        this.chips -= amount;
        this.currBet += amount;
        this.totalRoundBet += amount; // Sumujemy wkład do kartoteki
    }

    public void addChips(int amount) { this.chips += amount; }
    public void fold() { this.folded = true; }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getChips() { return chips; }
    public int getCurrBet() { return currBet; }
    public int getTotalRoundBet() { return totalRoundBet; }
    public Hand getHand() { return hand; }
    public boolean isFolded() { return folded; }
    public boolean hasActed() { return acted; }
    public void setActed(boolean acted) { this.acted = acted; }

    public void setHand(Hand hand) {
        this.hand = hand;
    }

}