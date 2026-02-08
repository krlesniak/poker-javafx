package client.gui;

import cards.Card;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class PlayerNode extends BorderPane {
    private final Label nameLabel;
    private final Label chipsLabel;
    private final HBox cardsBox;
    private List<Card> currentCards = new ArrayList<>();
    private final List<Boolean> selectedIndices = new ArrayList<>();
    private boolean cardsRevealed = false;
    private boolean isLocalPlayer = false;

    public PlayerNode(String name, int initialChips, boolean isLocalPlayer) {
        this.isLocalPlayer = isLocalPlayer;

        this.getStyleClass().add("player-node");
        String borderColor = isLocalPlayer ? "#2ecc71" : "#7f8c8d";
        this.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 2; -fx-border-radius: 10; -fx-padding: 10;");

        // DANE GRACZA: Nickname i Chipsy umieszczone w LEWYM GÓRNYM ROGU
        nameLabel = new Label(isLocalPlayer ? "TY: " + name : name);
        nameLabel.getStyleClass().add("player-name");
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        chipsLabel = new Label(initialChips + " $");
        chipsLabel.getStyleClass().add("player-chips");
        chipsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #f1c40f; -fx-font-weight: bold;");

        VBox infoBox = new VBox(2, nameLabel, chipsLabel);
        infoBox.setAlignment(Pos.TOP_LEFT); // Nick i kasa w lewym górnym rogu
        infoBox.setPadding(new Insets(5, 20, 0, 5));

        // KARTY: Teraz mają jeszcze więcej miejsca na środku
        cardsBox = new HBox(-60); // Większy wachlarz (nachodzenie na siebie)
        cardsBox.setAlignment(Pos.CENTER);
        cardsBox.setMinHeight(170);

        this.setLeft(infoBox);
        this.setCenter(cardsBox);

        this.setOnMouseClicked(e -> {
            if (isLocalPlayer && !currentCards.isEmpty()) {
                cardsRevealed = !cardsRevealed;
                refreshCards();
            }
        });
    }

    public void setCards(List<Card> cards) {
        this.currentCards = cards;
        this.selectedIndices.clear();
        for (int i = 0; i < cards.size(); i++) selectedIndices.add(false);
        this.cardsRevealed = false;
        refreshCards();
    }

    private void refreshCards() {
        cardsBox.getChildren().clear();
        for (int i = 0; i < currentCards.size(); i++) {
            final int index = i;
            Card card = currentCards.get(i);
            String path = cardsRevealed ? getCardPath(card) : "/images/cards/back.png";

            try {
                Image img = new Image(getClass().getResourceAsStream(path));
                ImageView view = new ImageView(img);

                // JESZCZE WIĘKSZE KARTY (160 fitHeight)
                view.setFitHeight(160);
                view.setPreserveRatio(true);

                view.setEffect(new DropShadow(10, Color.BLACK));

                if (selectedIndices.size() > i && selectedIndices.get(i)) {
                    view.setTranslateY(-30);
                    view.setEffect(new DropShadow(25, Color.GOLD));
                }

                view.setOnMouseClicked(e -> {
                    if (isLocalPlayer && cardsRevealed) {
                        selectedIndices.set(index, !selectedIndices.get(index));
                        refreshCards();
                        e.consume();
                    }
                });

                cardsBox.getChildren().add(view);
            } catch (Exception e) {
                System.err.println("Brak karty: " + path);
            }
        }
    }

    public List<Integer> getSelectedIndices() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < selectedIndices.size(); i++) {
            if (selectedIndices.get(i)) result.add(i);
        }
        return result;
    }

    private String getCardPath(Card card) {
        String rankStr = card.rank().name().toLowerCase();
        if (card.rank().getValue() <= 10) rankStr = String.valueOf(card.rank().getValue());
        return "/images/cards/" + rankStr + "_of_" + card.suit().name().toLowerCase() + ".png";
    }

    public void updateChips(int amount) {
        chipsLabel.setText(amount + " $");
    }
}