package client.gui;

import javafx.application.Application;

public class PokerGuiLauncher {
    public static void main(String[] args) {
        // Uruchamiamy PokerVisualApp zamiast starego PokerTableApp
        Application.launch(PokerVisualApp.class, args);
    }
}