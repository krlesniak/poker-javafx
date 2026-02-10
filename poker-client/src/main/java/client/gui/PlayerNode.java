package client.gui;

import cards.Card;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;

// view of the player's table
public class PlayerNode extends BorderPane {
    private final Label nameLabel;
    private final HBox chipsContainer;
    private final Label chipsValueLabel;
    private final Label currentBetLabel;
    private final ImageView avatarView;
    private final HBox cardsBox;
    private final Label handStrengthLabel;
    private final Label dealerButton;

    private List<Card> currentCards = new ArrayList<>();
    private final List<Boolean> selectedIndices = new ArrayList<>();
    private boolean cardsRevealed = false;
    private final boolean isLocalPlayer;
    private int currentChips = 0;
    private String currentHandStrength = "";

    private static final Image CHIP_YELLOW = loadChip("/images/chips/yellow.png");
    private static final Image CHIP_BLUE = loadChip("/images/chips/blue.png");
    private static final Image CHIP_GREEN = loadChip("/images/chips/green.png");
    private static final Image CHIP_RED = loadChip("/images/chips/red.png");

    private static Image loadChip(String path) {
        try { return new Image(PlayerNode.class.getResourceAsStream(path)); }
        catch (Exception e) { return null; }
    }

    public PlayerNode(String name, int initialChips, boolean isLocalPlayer) {
        this.isLocalPlayer = isLocalPlayer;
        this.getStyleClass().add("player-node");
        this.currentChips = initialChips;

        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        avatarView = new ImageView(loadAvatarImage(name));
        avatarView.setFitWidth(45); avatarView.setFitHeight(45);
        Circle clip = new Circle(22.5, 22.5, 22.5);
        avatarView.setClip(clip);
        avatarView.setEffect(new DropShadow(5, Color.BLACK));

        nameLabel = new Label(isLocalPlayer ? "YOU: " + name : name);
        nameLabel.getStyleClass().add("player-name");

        dealerButton = new Label("D");
        dealerButton.getStyleClass().add("dealer-button");
        dealerButton.setMinSize(26, 26);
        dealerButton.setMaxSize(26, 26);
        dealerButton.setVisible(false);

        HBox nameAndDealer = new HBox(8);
        nameAndDealer.setAlignment(Pos.CENTER_LEFT);
        nameAndDealer.getChildren().addAll(nameLabel, dealerButton);

        headerBox.getChildren().addAll(avatarView, nameAndDealer);

        chipsContainer = new HBox(-22);
        chipsContainer.setAlignment(Pos.CENTER_LEFT);
        chipsValueLabel = new Label(initialChips + " $");
        chipsValueLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-weight: bold;");
        currentBetLabel = new Label("BET: 0 $");
        currentBetLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 13px; -fx-font-weight: bold;");

        VBox infoBox = new VBox(3, headerBox, chipsContainer, chipsValueLabel, currentBetLabel);
        infoBox.setAlignment(Pos.TOP_LEFT);
        infoBox.setPadding(new Insets(5, 10, 5, 10));

        cardsBox = new HBox(-60);
        cardsBox.setAlignment(Pos.CENTER);
        cardsBox.setMinHeight(170);

        handStrengthLabel = new Label("");
        handStrengthLabel.getStyleClass().add("hand-strength-label");
        handStrengthLabel.setVisible(false);

        VBox centerStack = new VBox(5, cardsBox, handStrengthLabel);
        centerStack.setAlignment(Pos.CENTER);

        this.setLeft(infoBox);
        this.setCenter(centerStack);
        updateChips(initialChips);

        // animation of flipping all cards when click
        this.setOnMouseClicked(e -> {
            if (isLocalPlayer && !currentCards.isEmpty()) {
                cardsRevealed = !cardsRevealed;
                for (int i = 0; i < cardsBox.getChildren().size(); i++) {
                    Node node = cardsBox.getChildren().get(i);
                    if (node instanceof ImageView view) {
                        String path = cardsRevealed ? getCardPath(currentCards.get(i)) : "/images/cards/back.png";
                        CardAnimator.flipCard(view, new Image(getClass().getResourceAsStream(path)));
                    }
                }
                updateHandStrengthVisibility();
            }
        });
    }

    // choosing dealer with the white chip with 'D'
    public void setDealer(boolean isDealer) {
        Platform.runLater(() -> dealerButton.setVisible(isDealer));
    }

    public int getCurrentChips() { return currentChips; }

    // changing amount of chips every bet
    public void updateChips(int amount) {
        this.currentChips = amount;
        Platform.runLater(() -> {
            chipsValueLabel.setText(amount + " $");
            chipsContainer.getChildren().clear();
            int temp = amount;
            addChips(temp / 100, CHIP_YELLOW); temp %= 100;
            addChips(temp / 50, CHIP_BLUE); temp %= 50;
            addChips(temp / 25, CHIP_GREEN); temp %= 25;
            addChips(temp / 10, CHIP_RED);
        });
    }

    public void setHandStrength(String text) {
        this.currentHandStrength = text;
        Platform.runLater(this::updateHandStrengthVisibility);
    }

    // hand strength only visible after second bet phase and when cards are flipped upwards
    private void updateHandStrengthVisibility() {
        if (currentHandStrength == null || currentHandStrength.isEmpty() || !cardsRevealed) {
            handStrengthLabel.setVisible(false);
        } else {
            handStrengthLabel.setText(currentHandStrength);
            handStrengthLabel.setVisible(true);
        }
    }

    // player can not see oponnents cards
    public void showOpponentBacks() {
        if (isLocalPlayer) return;
        Platform.runLater(() -> {
            cardsBox.getChildren().clear();
            for (int i = 0; i < 5; i++) {
                try {
                    ImageView v = new ImageView(new Image(getClass().getResourceAsStream("/images/cards/back.png")));
                    v.setFitHeight(160); v.setPreserveRatio(true);
                    v.setEffect(new DropShadow(10, Color.BLACK));
                    cardsBox.getChildren().add(v);
                } catch (Exception e) {}
            }
        });
    }

    public void revealHand(List<Card> cards) {
        this.currentCards = cards;
        this.cardsRevealed = true;
        Platform.runLater(() -> {
            for (int i = 0; i < cardsBox.getChildren().size(); i++) {
                Node node = cardsBox.getChildren().get(i);
                if (node instanceof ImageView view && i < cards.size()) {
                    CardAnimator.flipCard(view, new Image(getClass().getResourceAsStream(getCardPath(cards.get(i)))));
                }
            }
            updateHandStrengthVisibility();
        });
    }

    public void resetRoundBet() { Platform.runLater(() -> currentBetLabel.setText("BET: 0 $")); }
    public void updateRoundBet(int totalAmount) { Platform.runLater(() -> currentBetLabel.setText("BET: " + totalAmount + " $")); }

    // loading for avatars for every player
    private Image loadAvatarImage(String playerName) {
        try {
            String numberPart = playerName.replaceAll("\\D+", "");
            int idx = 1;
            if (!numberPart.isEmpty()) {
                int playerNum = Integer.parseInt(numberPart);
                idx = Math.floorMod(playerNum - 1, 4) + 1;
            }
            return new Image(getClass().getResourceAsStream("/images/avatars/avatar" + idx + ".png"));
        } catch (Exception e) {
            return new Image(getClass().getResourceAsStream("/images/chips/yellow.png"));
        }
    }

    public void setCards(List<Card> cards) {
        this.currentCards = cards;
        this.selectedIndices.clear();
        for (int i = 0; i < cards.size(); i++) selectedIndices.add(false);
        refreshCards();
    }

    // giving different cards every round
    private void refreshCards() {
        cardsBox.getChildren().clear();
        for (int i = 0; i < currentCards.size(); i++) {
            final int idx = i;
            String path = cardsRevealed ? getCardPath(currentCards.get(i)) : "/images/cards/back.png";
            try {
                ImageView v = new ImageView(new Image(getClass().getResourceAsStream(path)));
                v.setFitHeight(160); v.setPreserveRatio(true);
                if (selectedIndices.size() > i && selectedIndices.get(i)) {
                    v.setTranslateY(-30);
                    v.setEffect(new DropShadow(25, Color.GOLD));
                } else {
                    v.setEffect(new DropShadow(10, Color.BLACK));
                }
                v.setOnMouseClicked(e -> {
                    if (isLocalPlayer && cardsRevealed) {
                        selectedIndices.set(idx, !selectedIndices.get(idx));
                        refreshCards();
                        e.consume();
                    }
                });
                cardsBox.getChildren().add(v);
            } catch (Exception e) {}
        }
    }

    private String getCardPath(Card card) {
        String r = card.rank().name().toLowerCase();
        if (card.rank().getValue() <= 10) r = String.valueOf(card.rank().getValue());
        return "/images/cards/" + r + "_of_" + card.suit().name().toLowerCase() + ".png";
    }

    // animation of taking card
    public void animateDraw(List<Card> newHand, Node deckNode, Pane root) {
        List<Node> oldNodesToDiscard = new ArrayList<>();
        List<Integer> indicesToReplace = new ArrayList<>();
        for (int i = 0; i < selectedIndices.size(); i++) {
            if (selectedIndices.get(i) && i < cardsBox.getChildren().size()) {
                oldNodesToDiscard.add(cardsBox.getChildren().get(i));
                indicesToReplace.add(i);
            }
        }
        CardAnimator.animateExchange(root, oldNodesToDiscard, deckNode, () -> {
            this.currentCards = newHand;
            this.selectedIndices.clear();
            for (int i = 0; i < newHand.size(); i++) selectedIndices.add(false);
            refreshCards();
            List<ImageView> viewsToAnimate = new ArrayList<>();
            for (int idx : indicesToReplace) {
                if (idx < cardsBox.getChildren().size()) {
                    Node n = cardsBox.getChildren().get(idx);
                    if (n instanceof ImageView) { n.setVisible(false); viewsToAnimate.add((ImageView) n); }
                }
            }
            return viewsToAnimate;
        });
    }

    private void addChips(int count, Image img) {
        if (img == null) return;
        for (int i = 0; i < Math.min(count, 8); i++) {
            ImageView v = new ImageView(img);
            v.setFitWidth(35); v.setFitHeight(35);
            v.setEffect(new DropShadow(5, Color.BLACK));
            chipsContainer.getChildren().add(v);
        }
    }

    public HBox getChipsContainer() { return chipsContainer; }
    public String getNameLabelText() { return nameLabel.getText(); }
    public List<Integer> getSelectedIndices() {
        List<Integer> r = new ArrayList<>();
        for (int i = 0; i < selectedIndices.size(); i++) if (selectedIndices.get(i)) r.add(i);
        return r;
    }
}