package client.gui;

import cards.Card;
import common.*;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import java.util.*;

public class PokerTableController {
    private final BorderPane mainLayout;
    private final VBox bottomContainer;
    private final Label phaseLabel;
    private final Map<String, PlayerNode> playerNodes = new HashMap<>();
    private String myId;
    private final Map<CommandType, Button> actionButtons = new HashMap<>();
    private Button btnRestart;

    public PokerTableController(BorderPane mainLayout, VBox bottomContainer, Label phaseLabel) {
        this.mainLayout = mainLayout;
        this.bottomContainer = bottomContainer;
        this.phaseLabel = phaseLabel;
    }

    public void registerButtons(Button fold, Button check, Button call, Button bet, Button draw, Button restart) {
        actionButtons.put(CommandType.FOLD, fold);
        actionButtons.put(CommandType.CHECK, check);
        actionButtons.put(CommandType.CALL, call);
        actionButtons.put(CommandType.BET, bet);
        actionButtons.put(CommandType.DRAW, draw);
        this.btnRestart = restart;
        disableAllActions();
    }

    public void handleServerCommand(GameCommand cmd) {
        switch (cmd.getType()) {
            case WELCOME -> this.myId = cmd.getParameters()[1];

            case JOIN -> {
                String id = cmd.getParameters()[0];
                String name = cmd.getParameters()[1];
                int chips = cmd.getIntParam(2);
                addOrUpdatePlayer(id, name, chips);
            }

            case DEAL -> {
                List<Card> cards = new ArrayList<>();
                for (String p : cmd.getParameters()) {
                    Card c = CardParser.fromString(p);
                    if (c != null) cards.add(c);
                }
                Platform.runLater(() -> {
                    if (playerNodes.containsKey(myId)) playerNodes.get(myId).setCards(cards);
                    if(btnRestart != null) btnRestart.setVisible(false);
                });
            }

            case TURN -> {
                String activeId = cmd.getParameters()[0];
                String name = cmd.getParameters()[1];
                String phase = cmd.getParameters()[2];
                Platform.runLater(() -> {
                    highlightActivePlayer(activeId);
                    phaseLabel.setText("FAZA: " + phase + " | KOLEJ: " + name);
                    updateButtonAvailability(activeId, phase);
                });
            }

            case WINNER -> {
                String winName = cmd.getParameters()[0];
                String winAmt = cmd.getParameters()[1];
                String winHand = cmd.getParameters().length > 2 ? cmd.getParameters()[2].replace("_", " ") : "";
                Platform.runLater(() -> {
                    phaseLabel.setText("WYGRYWA: " + winName + " (+" + winAmt + "$) " + winHand);
                    disableAllActions();
                    if(btnRestart != null) btnRestart.setVisible(true);
                });
            }

            case STARTED -> Platform.runLater(() -> {
                if(btnRestart != null) btnRestart.setVisible(false);
                phaseLabel.setText("NOWA RUNDA ROZPOCZĘTA");
            });
        }
    }

    private void updateButtonAvailability(String activeId, String phase) {
        disableAllActions();
        if (myId != null && myId.equals(activeId)) {
            if (phase.contains("BET")) {
                actionButtons.get(CommandType.FOLD).setDisable(false);
                actionButtons.get(CommandType.CHECK).setDisable(false);
                actionButtons.get(CommandType.CALL).setDisable(false);
                actionButtons.get(CommandType.BET).setDisable(false);
            } else if (phase.equals("DRAW")) {
                actionButtons.get(CommandType.DRAW).setDisable(false);
            }
        }
    }

    private void disableAllActions() {
        actionButtons.values().forEach(b -> b.setDisable(true));
    }

    private void addOrUpdatePlayer(String id, String name, int chips) {
        Platform.runLater(() -> {
            if (playerNodes.containsKey(id)) {
                playerNodes.get(id).updateChips(chips);
                return;
            }

            boolean isLocal = id.equals(myId);
            PlayerNode node = new PlayerNode(name, chips, isLocal);
            playerNodes.put(id, node);

            if (isLocal) {
                bottomContainer.getChildren().add(0, node);
            } else {
                if (mainLayout.getTop() == null) {
                    mainLayout.setTop(node);
                } else if (mainLayout.getLeft() == null) {
                    mainLayout.setLeft(node);
                } else if (mainLayout.getRight() == null) {
                    mainLayout.setRight(node);
                }
            }
        });
    }

    // NAPRAWIONE: Zmienia tylko border, nie psuje CSS
    private void highlightActivePlayer(String id) {
        playerNodes.forEach((nodeId, node) -> {
            boolean isActive = nodeId.equals(id);
            boolean isLocal = nodeId.equals(myId);

            // Kolor bordera: Biały (aktywny), Zielony (ja), Szary (inny)
            String borderColor = isActive ? "#ffffff" : (isLocal ? "#2ecc71" : "#7f8c8d");
            String borderWidth = isActive ? "4" : "2";

            // Efekt świecenia dla aktywnego gracza
            String effect = isActive ? "-fx-effect: dropshadow(three-pass-box, gold, 15, 0.5, 0, 0);" : "";

            node.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: " + borderWidth + "; -fx-border-radius: 10; " + effect);
        });
    }

    public List<Integer> getSelectedIndices() {
        return (myId != null && playerNodes.containsKey(myId)) ? playerNodes.get(myId).getSelectedIndices() : new ArrayList<>();
    }
}