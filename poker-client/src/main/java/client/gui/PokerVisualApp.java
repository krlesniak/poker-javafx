package client.gui;

import client.GameClient;
import common.CommandType;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;
import java.util.Random;

public class PokerVisualApp extends Application {
    private GameClient client;
    private PokerTableController controller;
    private String playerName;

    @Override
    public void start(Stage stage) {
        this.playerName = "Gracz_" + new Random().nextInt(1000);
        BorderPane root = new BorderPane();
        // Styl jest teraz w .root w CSS

        VBox centerArea = new VBox(20); // Zwiększony odstęp
        centerArea.setAlignment(Pos.CENTER);

        Label potLabel = new Label("PULA: 0 $");
        potLabel.getStyleClass().add("pot-label");

        Label phaseLabel = new Label("OCZEKIWANIE NA GRACZY...");
        phaseLabel.getStyleClass().add("phase-label");

        Button btnStartGame = new Button("START GRY");
        btnStartGame.getStyleClass().addAll("button", "button-start");
        btnStartGame.setOnAction(e -> client.sendMessage("START"));

        Button btnRestart = new Button("NOWA RUNDA");
        btnRestart.getStyleClass().addAll("button", "button-restart");
        btnRestart.setVisible(false);
        btnRestart.setOnAction(e -> client.sendMessage("RESTART"));

        centerArea.getChildren().addAll(potLabel, phaseLabel, btnStartGame, btnRestart);
        root.setCenter(centerArea);

        VBox bottomContainer = new VBox(10);
        bottomContainer.setAlignment(Pos.CENTER);
        bottomContainer.setPadding(new Insets(20)); // Większy margines od dołu
        root.setBottom(bottomContainer);

        HBox actionPanel = new HBox(15);
        actionPanel.setAlignment(Pos.CENTER);
        actionPanel.setPadding(new Insets(15));
        actionPanel.getStyleClass().add("action-panel"); // Styl z CSS

        Button btnFold = new Button("FOLD");
        btnFold.getStyleClass().add("button-fold");

        Button btnCheck = new Button("CHECK");
        btnCheck.getStyleClass().add("button-check");

        Button btnCall = new Button("CALL");
        Button btnBet = new Button("BET");
        Button btnDraw = new Button("DRAW");

        TextField betValue = new TextField("100");
        betValue.setPrefWidth(70);
        betValue.setAlignment(Pos.CENTER);

        actionPanel.getChildren().addAll(btnFold, btnCheck, btnCall, betValue, btnBet, btnDraw);
        bottomContainer.getChildren().add(actionPanel);

        controller = new PokerTableController(root, bottomContainer, phaseLabel);
        controller.registerButtons(btnFold, btnCheck, btnCall, btnBet, btnDraw, btnRestart);

        client = new GameClient("localhost", 7777);
        client.setOnCommandReceived(cmd -> {
            controller.handleServerCommand(cmd);
            if (cmd.getType() == CommandType.ROUND) {
                potLabel.setText("PULA: " + cmd.getParameters()[0] + " $");
            } else if (cmd.getType() == CommandType.STARTED) {
                btnStartGame.setVisible(false);
            } else if (cmd.getType() == CommandType.ERR) {
                phaseLabel.setText("BŁĄD: " + cmd.getMessageAllParams());
            }
        });

        // Obsługa przycisków
        btnFold.setOnAction(e -> client.sendMessage("FOLD"));
        btnCall.setOnAction(e -> client.sendMessage("CALL"));
        btnCheck.setOnAction(e -> client.sendMessage("CHECK"));
        btnBet.setOnAction(e -> client.sendMessage("BET " + betValue.getText()));
        btnDraw.setOnAction(e -> {
            List<Integer> selected = controller.getSelectedIndices();
            StringBuilder sb = new StringBuilder("DRAW");
            for (Integer i : selected) sb.append(" ").append(i);
            client.sendMessage(sb.toString());
        });

        try {
            client.start();
            client.sendMessage("JOIN game-1 " + playerName + " 1000");
        } catch (Exception e) {
            phaseLabel.setText("BŁĄD POŁĄCZENIA!");
        }

        Scene scene = new Scene(root, 1200, 900); // Nieco większe okno
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Poker Professional - " + playerName);
        stage.show();
    }

    @Override
    public void stop() { if (client != null) client.close(); }
}