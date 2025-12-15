package hierarchy;
import cards.Rank;
import java.util.Comparator;
import java.util.Map;

/**
 * A class of comparator used to sort the cards layouts to reveal who has the highest ranking
 * It is very crucial in the ties when two players have 'FULL_HOUSE' and then three the same cards are more important than two cards
 * (for example THREE KINGS AND TWO TENS > THREE QUEENS AND TWO ACES)
 */
public class RankComparator implements Comparator<Rank> {

    /**
     * A map that collects all the appearances of every rang in hand
     */
    private final Map<Rank, Integer> counts;

    /**
     * A constructor that initializes the map counts
     * @param counts a map that collects all the appearances of every rang in hand
     */
    // map that counts appearance of every rank
    public RankComparator(Map<Rank, Integer> counts) {
        this.counts = counts;
    }

    /**
     * A method that compares two ranks r1 and r2
     * @param r1 first rang to compare
     * @param r2 second rang to compare
     * @return a positive value if r1 is higher than r2, zero if there is a tie
     * or negative value if r1 is lower than r2
     */
    @Override
    public int compare(Rank r1, Rank r2) {
        int count1 = counts.get(r1);
        int count2 = counts.get(r2);

        // if other layouts
        if (count1 != count2) {
            return Integer.compare(count2, count1);
        }

        // if the same layout we compare base on the rank of cards
        return Integer.compare(r2.getValue(), r1.getValue());
    }
}