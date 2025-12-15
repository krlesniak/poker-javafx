package hierarchy;

import java.util.List;

/**
 * A class that represents the final strength and value of a 5 card poker hand.
 * @param ranking hand's category (for example FULL_HOUSE)
 * @param decider  sorted list of card values necessary for resolving ties.
 */
public record HandValue(HandRanking ranking, List<Integer> decider) implements Comparable<HandValue> {

    /**
     * A method that compares this hand value with another hand value for order.
     * @param other hand value to compare against
     * @return a positive value if our hand is higher than other hand, zero if there is a tie
     * or negative value if our hand is lower than other hand
     */
    @Override
    public int compareTo(HandValue other) {
        int rankCompare = Integer.compare(this.ranking.getValue(), other.ranking.getValue());
        if (rankCompare != 0) {
            return rankCompare;
        }
        // Situation where two or more players have the same ranking (for example each player have two pairs).
        // Then we compare the power of theirs pairs and decide which value is higher.
        for (int i = 0; i < this.decider.size(); i++) {
            if (i >= other.decider.size()) { break; }

            int val1 = this.decider.get(i);
            int val2 = other.decider.get(i);
            int diff = Integer.compare(val1, val2);
            if (val1 != val2) {
                return diff;
            }
        }
        // Draw - every card power is the same so we split the bet
        return 0;
    }

    /**
     * A method that returns a string representation of the hand value, including the rank name and
     * the list of tie-breaker values.
     * @return A string in the format 'RANKING_NAME [decider_values]'
     */
    @Override
    public String toString() {
        return ranking.toString() + " " + decider.toString();
    }
}
