package fr.iut.escape.client.ui;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Petites fabriques d'éléments d'interface, pour garder le même style partout. */
public final class Ui {

    /** Taille logique du jeu. */
    public static final double W = 1280;
    public static final double H = 720;
    /** Les décors font 3840x2160 px : ce facteur convertit leurs pixels en pixels d'écran. */
    public static final double ECHELLE = W / 3840.0;

    public static ImageView image(String nom, double largeur, double hauteur) {
        ImageView vue = new ImageView(new Image(Ui.class.getResource("/images/" + nom).toExternalForm()));
        vue.setFitWidth(largeur);
        vue.setFitHeight(hauteur);
        vue.setSmooth(false);
        return vue;
    }

    public static Label texte(String contenu, String classe) {
        Label label = new Label(contenu);
        label.getStyleClass().add(classe);
        label.setWrapText(true);
        return label;
    }

    public static Button bouton(String libelle, Runnable action) {
        Button bouton = new Button(libelle);
        bouton.setOnAction(e -> action.run());
        return bouton;
    }

    public static Button boutonDanger(String libelle, Runnable action) {
        Button bouton = bouton(libelle, action);
        bouton.getStyleClass().add("danger");
        return bouton;
    }

    public static HBox ligne(Node... noeuds) {
        HBox ligne = new HBox(16, noeuds);
        ligne.setAlignment(Pos.CENTER);
        return ligne;
    }

    /** Calque plein écran qui centre son contenu ; avec voile = true, il assombrit et bloque le décor. */
    public static StackPane centre(boolean voile, Node contenu) {
        StackPane calque = new StackPane(contenu);
        calque.setPrefSize(W, H);
        if (voile) {
            calque.getStyleClass().add("voile");
        }
        return calque;
    }

    /** Fenêtre du jeu (popup) : un panneau centré sur fond assombri. */
    public static StackPane popup(double largeur, Node... contenu) {
        VBox panneau = new VBox(18, contenu);
        panneau.getStyleClass().add("panneau");
        panneau.setAlignment(Pos.CENTER);
        panneau.setMaxSize(largeur, Region.USE_PREF_SIZE);
        return centre(true, panneau);
    }

    /** Zone cliquable posée sur le décor, en pixels de l'image d'origine. */
    public static Region zone(double x, double y, double largeur, double hauteur, Runnable action) {
        Region zone = new Region();
        zone.getStyleClass().add("zone");
        zone.setLayoutX(x * ECHELLE);
        zone.setLayoutY(y * ECHELLE);
        zone.setPrefSize(largeur * ECHELLE, hauteur * ECHELLE);
        zone.setCursor(Cursor.HAND);
        zone.setOnMouseClicked(e -> action.run());
        return zone;
    }

    /** Fait clignoter un élément pour attirer l'œil. */
    public static void clignoter(Node noeud) {
        FadeTransition fondu = new FadeTransition(Duration.millis(600), noeud);
        fondu.setFromValue(1);
        fondu.setToValue(0.25);
        fondu.setAutoReverse(true);
        fondu.setCycleCount(Animation.INDEFINITE);
        fondu.play();
    }

    /** Popup avec barre de progression ; appelle fin quand la durée est écoulée. */
    public static StackPane chargement(String titre, String detail, int secondes, Runnable fin) {
        ProgressBar barre = new ProgressBar(0);
        barre.setPrefWidth(460);
        Timeline progression = new Timeline(
                new KeyFrame(Duration.seconds(secondes), new KeyValue(barre.progressProperty(), 1)));
        progression.setOnFinished(e -> fin.run());
        progression.play();
        barre.sceneProperty().addListener((obs, avant, apres) -> {
            if (avant != null && apres == null) {
                progression.stop();
            }
        });
        return popup(560, texte(titre, "titre-popup"), texte(detail, "normal"), barre);
    }

    private Ui() {
    }
}
