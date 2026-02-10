package client.gui;

import cards.Card;
import common.*;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import java.util.*;

public class PokerTableController {
    private final HBox topPlayersBox;
    private final HBox bottomPlayersBox;
    private final Label phaseLabel;
    private final Label turnLabel;
    private final Label potLabelText;
    private final Node deckNode;
    private final ListView<String> logList;

    private final Map<String, PlayerNode> playerNodes = new HashMap<>();
    private String myId;
    private String hostId;
    private final Map<CommandType, Button> actionButtons = new HashMap<>();
    private Button btnRestart;
    private Button btnStartGame;
    private Button btnFullReset;
    private String currentPhase = "";

    public PokerTableController(HBox topBox, HBox bottomBox, Label phaseLabel, Label turnLabel, Label potLabel, Node deck, ListView<String> logs) {
        this.topPlayersBox = topBox;
        this.bottomPlayersBox = bottomBox;
        this.phaseLabel = phaseLabel;
        this.turnLabel = turnLabel;
        this.potLabelText = potLabel;
        this.deckNode = deck;
        this.logList = logs;
    }

    // all buttons at the bottom of the screen
    public void registerButtons(Button fold, Button check, Button call, Button bet, Button draw, Button restart, Button start, Button reset) {
        actionButtons.put(CommandType.FOLD, fold);
        actionButtons.put(CommandType.CHECK, check);
        actionButtons.put(CommandType.CALL, call);
        actionButtons.put(CommandType.BET, bet);
        actionButtons.put(CommandType.DRAW, draw);
        this.btnRestart = restart;
        this.btnStartGame = start;
        this.btnFullReset = reset;
    }

    public int getLocalPlayerChips() { return (myId != null && playerNodes.containsKey(myId)) ? playerNodes.get(myId).getCurrentChips() : 0; }

    public void animateLocalBet(int amount) {
        if (myId != null && playerNodes.containsKey(myId)) {
            Node pot = PokerVisualApp.rootPane.lookup("#potLabelCenter");
            ChipAnimator.animateBet(PokerVisualApp.rootPane, playerNodes.get(myId).getChipsContainer(), pot, amount);
        }
    }

    // handling every server command type from the nio server
    public void handleServerCommand(GameCommand cmd) {
        switch (cmd.getType()) {
            // joining the lobby
            case WELCOME -> {
                this.myId = cmd.getParameters()[1];
                this.hostId = cmd.getParameters()[2];
                Platform.runLater(() -> { if (myId.equals(hostId)) btnStartGame.setVisible(true); });
            }
            case JOIN -> addOrUpdatePlayer(cmd.getParameters()[0], cmd.getParameters()[1], cmd.getIntParam(2));
            // window logs on the right of thge scceen with every action for the players so the gameplay is easier
            case LOG -> {
                String msg = cmd.getMessageAllParams().replace("_", " ");
                Platform.runLater(() -> { logList.getItems().add(msg); logList.scrollTo(logList.getItems().size() - 1); });
            }
            // start of the round
            case STARTED -> Platform.runLater(() -> {
                logList.getItems().clear();
                if (btnRestart != null) btnRestart.setVisible(false);
                if (btnFullReset != null) btnFullReset.setVisible(false);
                if (PokerVisualApp.btnShowdown != null) PokerVisualApp.btnShowdown.setVisible(false);
            });
            // choosing a dealer, every round changes on the next player
            case DEALER -> {
                String dealerId = cmd.getParameters()[0];
                Platform.runLater(() -> playerNodes.forEach((id, node) -> node.setDealer(id.equals(dealerId))));
            }

            // dealer gives every player five cards
            case DEAL -> {
                List<Card> cards = new ArrayList<>(); String handStr = "";
                for (String p : cmd.getParameters()) { if (p.startsWith("STR:")) { handStr = p.substring(4).replace("_", " "); continue; } Card c = CardParser.fromString(p); if (c != null) cards.add(c); }
                String finalHandStr = handStr;
                Platform.runLater(() -> {
                    if (playerNodes.containsKey(myId)) {
                        if (currentPhase.equals("DRAW") && !playerNodes.get(myId).getSelectedIndices().isEmpty()) playerNodes.get(myId).animateDraw(cards, deckNode, PokerVisualApp.rootPane);
                        else playerNodes.get(myId).setCards(cards);
                        playerNodes.get(myId).setHandStrength(finalHandStr);
                    }
                    playerNodes.forEach((id, node) -> { if (!id.equals(myId)) node.showOpponentBacks(); });
                });
            }

            // when a player clicks showdown button
            case READY_SHOWDOWN -> {
                String dId = cmd.getParameters()[0];
                Platform.runLater(() -> { if (myId.equals(dId)) PokerVisualApp.btnShowdown.setVisible(true); });
            }

            // revealing who is teh winnr after the showdown
            case REVEAL -> {
                String pId = cmd.getParameters()[0]; List<Card> cards = new ArrayList<>();
                for (int i = 1; i < cmd.getParameters().length; i++) { Card c = CardParser.fromString(cmd.getParameters()[i]); if (c != null) cards.add(c); }
                if (playerNodes.containsKey(pId)) playerNodes.get(pId).revealHand(cards);
            }

            // choosing who is the winner based on the players' hands
            case WINNER -> {
                PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                pause.setOnFinished(e -> Platform.runLater(() -> {
                    String winnerName = cmd.getParameters()[0];
                    phaseLabel.setText("WINNER: " + winnerName);
                    PlayerNode winnerNode = playerNodes.values().stream().filter(n -> n.getNameLabelText().contains(winnerName)).findFirst().orElse(null);
                    HBox potBox = (HBox) PokerVisualApp.rootPane.lookup("#potChipsBox");
                    if (winnerNode != null && potBox != null) ChipAnimator.animatePotToWinner(PokerVisualApp.rootPane, potBox, winnerNode);
                    if (myId != null && myId.equals(hostId)) { PokerVisualApp.btnRestart.setVisible(true); PokerVisualApp.btnFullReset.setVisible(true); }
                    PauseTransition rainDelay = new PauseTransition(Duration.seconds(1.0));
                    rainDelay.setOnFinished(ev -> ChipAnimator.animateChipRain(PokerVisualApp.overlayPane));
                    rainDelay.play();
                }));
                pause.play();
            }

            // whose turn it is now
            case TURN -> {
                String activeId = cmd.getParameters()[0];
                String phase = cmd.getParameters()[2];
                Platform.runLater(() -> {
                    this.currentPhase = phase;
                    highlightActivePlayer(activeId); phaseLabel.setText("PHASE: " + phase);
                    turnLabel.setText("TURN: " + cmd.getParameters()[1]);
                    updateButtonAvailability(activeId, phase);
                });
            }
            // what action of the round
            case ACTION -> {
                String pId = cmd.getParameters()[0];
                int totalRoundSum = cmd.getIntParam(2);
                Platform.runLater(() -> { if (playerNodes.containsKey(pId)) playerNodes.get(pId).updateRoundBet(totalRoundSum); });
            }
            // strength of cards in the player's hand
            case STRENGTH -> { String pId = cmd.getParameters()[0];
                String text = cmd.getMessageAllParams().substring(pId.length()).trim().replace("_", " ");
                Platform.runLater(() -> { if (playerNodes.containsKey(pId)) playerNodes.get(pId).setHandStrength(text); });
            }
        }
    }

    // visibility of the buttons based on the phase
    private void updateButtonAvailability(String activeId, String phase) {
        actionButtons.values().forEach(b -> b.setDisable(true));
        if (myId != null && myId.equals(activeId)) {
            if (phase.contains("BET")) {
                actionButtons.get(CommandType.FOLD).setDisable(false);
                actionButtons.get(CommandType.CHECK).setDisable(false);
                actionButtons.get(CommandType.CALL).setDisable(false);
                actionButtons.get(CommandType.BET).setDisable(false); }
            else if (phase.equals("DRAW")) actionButtons.get(CommandType.DRAW).setDisable(false);
        }
    }

    private void addOrUpdatePlayer(String id, String name, int chips) {
        Platform.runLater(() -> {
            if (playerNodes.containsKey(id)) { playerNodes.get(id).updateChips(chips); return; }
            PlayerNode node = new PlayerNode(name, chips, id.equals(myId)); playerNodes.put(id, node);
            node.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(node, Priority.ALWAYS);
            if (id.equals(myId)) bottomPlayersBox.getChildren().add(node);
            else { if (topPlayersBox.getChildren().size() < 2) topPlayersBox.getChildren().add(node); else bottomPlayersBox.getChildren().add(0, node); }
        });
    }

    // nice yellow highligh around the table whenever it is player's turn
    private void highlightActivePlayer(String id) {
        playerNodes.forEach((nodeId, node) -> {
            boolean active = nodeId.equals(id);
            String effect = active ? "-fx-effect: dropshadow(three-pass-box, gold, 35, 0.7, 0, 0);" : "";
            node.setStyle("-fx-background-color: rgba(30, 30, 30, 0.85); -fx-background-radius: 10; -fx-border-color: " + (active ? "white" : "#7f8c8d") + "; -fx-border-width: " + (active ? "4" : "2") + "; -fx-border-radius: 10; -fx-padding: 10; " + effect);
        });
    }
    public List<Integer> getSelectedIndices() { return myId != null && playerNodes.containsKey(myId) ? playerNodes.get(myId).getSelectedIndices() : new ArrayList<>(); }
}