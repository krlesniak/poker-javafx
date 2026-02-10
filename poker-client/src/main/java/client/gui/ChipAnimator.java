package client.gui;

import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChipAnimator {
    private static final Image CHIP_YELLOW = load("/images/chips/yellow.png");
    private static final Image CHIP_BLUE = load("/images/chips/blue.png");
    private static final Image CHIP_GREEN = load("/images/chips/green.png");
    private static final Image CHIP_RED = load("/images/chips/red.png");
    private static final Image[] ALL_CHIPS = {CHIP_YELLOW, CHIP_BLUE, CHIP_GREEN, CHIP_RED};
    private static final Random RAND = new Random();

    private static Image load(String path) {
        try { return new Image(ChipAnimator.class.getResourceAsStream(path)); }
        catch (Exception e) { return null; }
    }

    // chips rain at the end of the round
    public static void animateChipRain(Pane root) {
        ParallelTransition rainAnim = new ParallelTransition();
        List<Node> drops = new ArrayList<>();
        double sceneWidth = root.getWidth();
        double sceneHeight = root.getHeight();

        for (int i = 0; i < 50; i++) {
            Image randomImg = ALL_CHIPS[RAND.nextInt(ALL_CHIPS.length)];
            if (randomImg == null) continue;

            ImageView chip = new ImageView(randomImg);
            chip.setFitWidth(40); chip.setFitHeight(40);

            // random position at the top
            double startX = RAND.nextDouble() * sceneWidth;
            chip.setTranslateX(startX);
            chip.setTranslateY(-50);
            chip.setMouseTransparent(true);

            root.getChildren().add(chip);
            drops.add(chip);

            // falling animation
            TranslateTransition tt = new TranslateTransition(
                    Duration.millis(1500 + RAND.nextInt(1000)), chip
            );
            tt.setToY(sceneHeight + 50); // falling under the screen
            tt.setDelay(Duration.millis(RAND.nextInt(500)));

            rainAnim.getChildren().add(tt);
        }

        rainAnim.setOnFinished(e -> root.getChildren().removeAll(drops));
        rainAnim.play();
    }

    public static void animateBet(Pane root, Node fromNode, Node toNode, int amount) {
        animateAndVanish(root, fromNode, toNode, amount);
    }

    public static void animatePayout(Pane root, Node fromNode, Node toNode, int amount) {
        animateAndVanish(root, fromNode, toNode, amount);
    }

    private static void animateAndVanish(Pane root, Node fromNode, Node toNode, int amount) {
        if (fromNode == null || toNode == null || amount <= 0) return;
        Point2D start = getAbsoluteCenter(fromNode);
        Point2D end = getAbsoluteCenter(toNode);
        List<Image> chips = getChipsForAmount(amount);
        ParallelTransition fullAnim = new ParallelTransition();
        List<Node> flyingChips = new ArrayList<>();
        int offset = 0;
        for (Image img : chips) {
            ImageView fly = new ImageView(img);
            fly.setFitWidth(35); fly.setFitHeight(35);
            fly.setTranslateX(start.getX() + offset);
            fly.setTranslateY(start.getY());
            fly.setMouseTransparent(true);
            root.getChildren().add(fly);
            flyingChips.add(fly);
            TranslateTransition tt = new TranslateTransition(Duration.millis(700), fly);
            tt.setToX(end.getX());
            tt.setToY(end.getY());
            fullAnim.getChildren().add(tt);
            offset += 5;
        }
        fullAnim.setOnFinished(e -> root.getChildren().removeAll(flyingChips));
        fullAnim.play();
    }

    private static List<Image> getChipsForAmount(int amount) {
        List<Image> list = new ArrayList<>();
        int t = amount;
        if (t >= 100) { list.add(CHIP_YELLOW); t-=100; }
        if (t >= 50) { list.add(CHIP_BLUE); t-=50; }
        if (t >= 25) { list.add(CHIP_GREEN); t-=25; }
        while (t >= 10) { list.add(CHIP_RED); t-=10; }
        if (list.isEmpty() && amount > 0) list.add(CHIP_RED);
        return list;
    }

    private static Point2D getAbsoluteCenter(Node node) {
        Bounds b = node.localToScene(node.getBoundsInLocal());
        return new Point2D(b.getMinX() + b.getWidth()/2, b.getMinY() + b.getHeight()/2);
    }

    // visual pool of chips in the center of the screen
    public static void updateVisualPot(javafx.scene.layout.HBox potBox, int amount) {
        if (potBox == null) return;
        javafx.application.Platform.runLater(() -> {
            potBox.getChildren().clear();
            List<Image> chips = getChipsForAmount(amount);

            // limit of 20 chips so it will not be too much
            int displayed = 0;
            for (Image img : chips) {
                if (displayed++ > 20) break;
                ImageView v = new ImageView(img);
                v.setFitWidth(30); v.setFitHeight(30);
                // stack effect
                v.setTranslateY(RAND.nextInt(10) - 5);
                v.setEffect(new javafx.scene.effect.DropShadow(5, javafx.scene.paint.Color.BLACK));
                potBox.getChildren().add(v);
            }
        });
    }

    // chips flying to the winner animation
    public static void animatePotToWinner(Pane root, javafx.scene.layout.HBox potBox, Node winnerNode) {
        if (potBox == null || winnerNode == null || potBox.getChildren().isEmpty()) return;

        Point2D end = getAbsoluteCenter(winnerNode);
        ParallelTransition flyAnim = new ParallelTransition();
        List<Node> chipsToMove = new ArrayList<>(potBox.getChildren());

        for (Node chip : chipsToMove) {
            Point2D start = getAbsoluteCenter(chip);

            potBox.getChildren().remove(chip);
            root.getChildren().add(chip);
            chip.setTranslateX(start.getX() - 15);
            chip.setTranslateY(start.getY() - 15);

            TranslateTransition tt = new TranslateTransition(Duration.millis(800), chip);
            tt.setToX(end.getX());
            tt.setToY(end.getY());
            tt.setOnFinished(e -> root.getChildren().remove(chip));
            flyAnim.getChildren().add(tt);
        }
        flyAnim.play();
    }
}