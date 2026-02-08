package common;

import cards.Card;
import cards.Rank;
import cards.Suit;

/**
 * Klasa narzędziowa do konwersji między tekstową reprezentacją karty a obiektem Card.
 */
public class CardParser {

    private CardParser() {}

    /**
     * Parsuje string w formacie RANK_SUIT (np. ACE_SPADES) na obiekt Card.
     * @param cardStr tekstowa reprezentacja karty
     * @return obiekt Card lub null w przypadku błędu
     */
    public static Card fromString(String cardStr) {
        if (cardStr == null || !cardStr.contains("_")) return null;
        try {
            String[] parts = cardStr.split("_");
            Rank rank = Rank.valueOf(parts[0].toUpperCase());
            Suit suit = Suit.valueOf(parts[1].toUpperCase());
            return new Card(rank, suit);
        } catch (Exception e) {
            System.err.println("Błąd parsowania karty: " + cardStr);
            return null;
        }
    }
}