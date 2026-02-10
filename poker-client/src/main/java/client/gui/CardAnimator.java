package client.gui;

import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import java.util.List;

public class CardAnimator {

    public interface NewCardsProvider {
        List<ImageView> prepareNewHandAndGetViewsToAnimate();
    }

    public static void animateExchange(Pane root, List<Node> oldNodes, Node deckNode, NewCardsProvider midCallback) {
        if (deckNode == null || oldNodes.isEmpty()) {
            if (midCallback != null) {
                midCallback.prepareNewHandAndGetViewsToAnimate().forEach(n -> n.setVisible(true));
            }
            return;
        }

        Point2D deckPos = getAbsoluteCenter(deckNode);
        ParallelTransition discardAnim = new ParallelTransition();

        // cards fly to the hand animation
        for (Node card : oldNodes) {
            Point2D start = getAbsoluteCenter(card);

            ImageView fly = copyImageView((ImageView) card);
            fly.setFitWidth(((ImageView) card).getFitWidth());
            fly.setFitHeight(((ImageView) card).getFitHeight());
            fly.setTranslateX(start.getX());
            fly.setTranslateY(start.getY());

            root.getChildren().add(fly);
            card.setVisible(false); // chosen cards disappear from the hand

            TranslateTransition tt = new TranslateTransition(Duration.millis(800), fly);
            tt.setToX(deckPos.getX());
            tt.setToY(deckPos.getY());

            tt.setOnFinished(e -> root.getChildren().remove(fly));
            discardAnim.getChildren().add(tt);
        }

        discardAnim.setOnFinished(e -> {
            // break between actions
            PauseTransition pause = new PauseTransition(Duration.millis(400));

            pause.setOnFinished(ev -> {
                // loading new cards from playerNode
                List<ImageView> targets = midCallback.prepareNewHandAndGetViewsToAnimate();

                root.layout();

                // giving new exchanged cards to the player's hand
                startDealingPhase(root, deckPos, targets);
            });

            pause.play();
        });

        discardAnim.play();
    }

    private static void startDealingPhase(Pane root, Point2D deckPos, List<ImageView> targets) {
        ParallelTransition dealAnim = new ParallelTransition();

        for (ImageView targetCard : targets) {
            Point2D end = getAbsoluteCenter(targetCard);

            ImageView fly = copyImageView(targetCard);
            fly.setTranslateX(deckPos.getX());
            fly.setTranslateY(deckPos.getY());

            root.getChildren().add(fly);
            targetCard.setVisible(false);

            TranslateTransition tt = new TranslateTransition(Duration.millis(600), fly);
            tt.setToX(end.getX());
            tt.setToY(end.getY());

            tt.setOnFinished(ev -> {
                root.getChildren().remove(fly);
                targetCard.setVisible(true);
            });
            dealAnim.getChildren().add(tt);
        }

        dealAnim.play();
    }

    private static ImageView copyImageView(ImageView original) {
        ImageView copy = new ImageView(original.getImage());
        copy.setFitWidth(original.getFitWidth());
        copy.setFitHeight(original.getFitHeight());
        copy.setPreserveRatio(original.isPreserveRatio());
        copy.setEffect(original.getEffect());
        copy.setMouseTransparent(true);
        return copy;
    }

    private static Point2D getAbsoluteCenter(Node node) {
        // cards fly to the center of the table
        Bounds b = node.localToScene(node.getBoundsInLocal());
        return new Point2D(b.getMinX() + b.getWidth() / 2, b.getMinY() + b.getHeight() / 2);
    }

    // flipping card animation for aesthetic reasons
    public static void flipCard(ImageView view, Image newImage) {
        javafx.animation.ScaleTransition st1 = new javafx.animation.ScaleTransition(Duration.millis(150), view);
        st1.setToX(0); // narrowing to the zero
        st1.setOnFinished(e -> {
            view.setImage(newImage); // change of the image in the middle of teh animation
            javafx.animation.ScaleTransition st2 = new javafx.animation.ScaleTransition(Duration.millis(150), view);
            st2.setToX(1); // extension back
            st2.play();
        });
        st1.play();
    }
}