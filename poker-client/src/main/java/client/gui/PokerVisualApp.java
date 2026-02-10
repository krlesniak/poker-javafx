package client.gui;

import client.GameClient;
import common.CommandType;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class PokerVisualApp extends Application {
    private GameClient client;
    private PokerTableController controller;
    public static BorderPane rootPane;
    public static Pane overlayPane;
    public static Button btnShowdown;
    public static Button btnRestart;
    public static Button btnFullReset;
    private boolean gameRunning = false;

    @Override
    public void start(Stage stage) {
        StackPane mainContainer = new StackPane();
        rootPane = new BorderPane();
        rootPane.getStyleClass().add("root");
        overlayPane = new Pane();
        overlayPane.setMouseTransparent(true);
        mainContainer.getChildren().addAll(rootPane, overlayPane);

        // main screen , center of the table
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER);

        // pool box
        StackPane potContainer = new StackPane();
        potContainer.setMinSize(200, 60);
        HBox potChipsBox = new HBox(-25); potChipsBox.setId("potChipsBox"); potChipsBox.setAlignment(Pos.CENTER); potChipsBox.setMouseTransparent(true);
        Label potLabel = new Label("POOL: 0 $"); potLabel.getStyleClass().add("pot-label"); potLabel.setId("potLabelCenter");
        potLabel.setTranslateY(-35);
        potContainer.getChildren().addAll(potChipsBox, potLabel);

        // signs with phase and whose turn it is
        Label phaseLabel = new Label("WAITING..."); phaseLabel.getStyleClass().add("phase-label");
        Label turnLabel = new Label("TURN: -"); turnLabel.getStyleClass().add("phase-label"); turnLabel.setStyle("-fx-text-fill: #f1c40f;");

        Button btnStartGame = new Button("START GAME"); btnStartGame.getStyleClass().add("button-start"); btnStartGame.setVisible(false);
        btnStartGame.setOnAction(e -> client.sendMessage("START"));

        btnRestart = new Button("NEW ROUND"); btnRestart.getStyleClass().add("button-restart");
        btnRestart.setStyle("-fx-font-size: 18px; -fx-padding: 8 30;"); btnRestart.setVisible(false);
        btnRestart.setOnAction(e -> client.sendMessage("RESTART"));

        btnFullReset = new Button("RESET GAME");
        btnFullReset.getStyleClass().add("button-full-reset");
        btnFullReset.setVisible(false);
        btnFullReset.setOnAction(e -> client.sendMessage("FULL_RESET"));

        // showdown button visual
        btnShowdown = new Button("SHOWDOWN");
        btnShowdown.getStyleClass().add("button-showdown");
        btnShowdown.setVisible(false);
        VBox.setMargin(btnShowdown, new Insets(5, 0, 5, 0));
        btnShowdown.setOnAction(e -> { client.sendMessage("SHOWDOWN"); btnShowdown.setVisible(false); });

        infoBox.getChildren().addAll(potContainer, phaseLabel, turnLabel, btnStartGame, btnRestart, btnFullReset, btnShowdown);

        // deck image
        ImageView deckView = new ImageView();
        try {
            deckView.setImage(new Image(getClass().getResourceAsStream("/images/cards/back.png")));
            deckView.setFitHeight(160);
            deckView.setPreserveRatio(true);
            deckView.setEffect(new javafx.scene.effect.DropShadow(15, Color.BLACK));
        } catch (Exception e) {}

        HBox middleRow = new HBox(180);
        middleRow.setAlignment(Pos.CENTER);
        middleRow.getChildren().addAll(deckView, infoBox);

        VBox centerArea = new VBox(15);
        centerArea.setAlignment(Pos.CENTER);
        centerArea.setPickOnBounds(false);
        centerArea.getChildren().add(middleRow);
        rootPane.setCenter(centerArea);

        // player layout and logs window
        HBox topPlayersBox = new HBox(0); topPlayersBox.setAlignment(Pos.TOP_CENTER); rootPane.setTop(topPlayersBox);
        VBox bottomWrapper = new VBox(5); bottomWrapper.setAlignment(Pos.BOTTOM_CENTER);
        HBox bottomPlayersBox = new HBox(0); bottomPlayersBox.setAlignment(Pos.BOTTOM_CENTER);
        HBox actionPanel = new HBox(15); actionPanel.setAlignment(Pos.CENTER); actionPanel.setPadding(new Insets(10)); actionPanel.getStyleClass().add("action-panel");
        Button btnFold = new Button("FOLD"); btnFold.getStyleClass().add("button-fold");
        Button btnCheck = new Button("CHECK"); btnCheck.getStyleClass().add("button-check");
        Button btnCall = new Button("CALL");
        Button btnBet = new Button("BET");
        Button btnDraw = new Button("DRAW");
        TextField betValue = new TextField("10"); betValue.setPrefWidth(80); betValue.setAlignment(Pos.CENTER);
        actionPanel.getChildren().addAll(btnFold, btnCheck, btnCall, betValue, btnBet, btnDraw);
        bottomWrapper.getChildren().addAll(bottomPlayersBox, actionPanel);
        rootPane.setBottom(bottomWrapper);

        ListView<String> logList = new ListView<>(); logList.getStyleClass().add("log-box"); logList.setPrefWidth(240);
        logList.setCellFactory(lv -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle("-fx-background-color: transparent;"); }
                else { setText(item); setStyle("-fx-text-fill: #ecf0f1; -fx-background-color: transparent;"); getStyleClass().add("log-list-cell"); }
            }
        });
        VBox logWrapper = new VBox(logList); logWrapper.setPadding(new Insets(10)); logWrapper.setAlignment(Pos.CENTER_RIGHT);
        rootPane.setRight(logWrapper);

        controller = new PokerTableController(topPlayersBox, bottomPlayersBox, phaseLabel, turnLabel, potLabel, deckView, logList);
        controller.registerButtons(btnFold, btnCheck, btnCall, btnBet, btnDraw, btnRestart, btnStartGame, btnFullReset);

        // connecting to teh server
        client = new GameClient("localhost", 7777);
        client.setOnCommandReceived(cmd -> {
            javafx.application.Platform.runLater(() -> phaseLabel.setTextFill(Color.WHITE));
            controller.handleServerCommand(cmd);
            if (cmd.getType() == CommandType.ROUND) {
                int amount = Integer.parseInt(cmd.getParameters()[0]);
                potLabel.setText("POOL: " + amount + " $");
                HBox potBox = (HBox) rootPane.lookup("#potChipsBox");
                ChipAnimator.updateVisualPot(potBox, amount);
            } else if (cmd.getType() == CommandType.STARTED) {
                gameRunning = true; btnStartGame.setVisible(false); btnRestart.setVisible(false); btnFullReset.setVisible(false);
            } else if (cmd.getType() == CommandType.WINNER) {
                gameRunning = false;
            } else if (cmd.getType() == CommandType.ERR) {
                javafx.application.Platform.runLater(() -> { phaseLabel.setText("ERR: " + cmd.getMessageAllParams()); phaseLabel.setTextFill(Color.RED); });
            }
        });

        // bet button actions
        btnBet.setOnAction(e -> {
            if (!gameRunning) return;
            try {
                int amt = Integer.parseInt(betValue.getText());
                int myChips = controller.getLocalPlayerChips();
                if (!isValidChipAmount(amt) || amt > myChips) { phaseLabel.setText(amt > myChips ? "TOO MUCH!" : "INVALID AMOUNT!"); phaseLabel.setTextFill(Color.RED); return; }
                client.sendMessage("BET " + amt); controller.animateLocalBet(amt);
            } catch (Exception ex) { phaseLabel.setText("INVALID NUMBER"); phaseLabel.setTextFill(Color.RED); }
        });

        btnCall.setOnAction(e -> { if (gameRunning) { client.sendMessage("CALL"); controller.animateLocalBet(10); } });
        btnFold.setOnAction(e -> { if (gameRunning) client.sendMessage("FOLD"); });
        btnCheck.setOnAction(e -> { if (gameRunning) client.sendMessage("CHECK"); });
        btnDraw.setOnAction(e -> {
            if (!gameRunning) return;
            StringBuilder sb = new StringBuilder("DRAW");
            for (Integer i : controller.getSelectedIndices()) sb.append(" ").append(i);
            client.sendMessage(sb.toString());
        });

        try { client.start(); client.sendMessage("JOIN game-1 Waiting... 1000"); } catch (Exception e) {}

        stage.setScene(new Scene(mainContainer, 1280, 900));
        stage.getScene().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setTitle("Poker Professional");
        stage.show();
    }

    private boolean isValidChipAmount(int amount) { return amount >= 10 && (amount % 10 == 0 || (amount >= 25 && (amount - 25) % 10 == 0)); }
    @Override
    public void stop() { if (client != null) client.close(); }
}