package fr.iut.escape.client.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Barre de dialogue en bas de l'écran, avec le nom de celui qui parle. */
public final class Dialogue extends VBox {

    public record Replique(String orateur, String texte) {
    }

    private final Label orateur = Ui.texte("", "orateur");
    private final Label texte = Ui.texte("", "normal");
    private final Button suivant = new Button();

    public Dialogue() {
        getStyleClass().add("dialogue");
        setLayoutX(40);
        setLayoutY(540);
        setPrefSize(Ui.W - 80, 160);
        setSpacing(8);
        VBox.setVgrow(texte, Priority.ALWAYS);
        texte.setMaxHeight(Double.MAX_VALUE);
        texte.setAlignment(Pos.TOP_LEFT);
        HBox pied = new HBox(suivant);
        pied.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(orateur, texte, pied);
    }

    /** Enchaîne les répliques, une par clic sur « Suivant », puis appelle fin. */
    public void jouer(List<Replique> repliques, Runnable fin) {
        jouer(repliques, 0, fin);
    }

    private void jouer(List<Replique> repliques, int index, Runnable fin) {
        if (index >= repliques.size()) {
            fin.run();
            return;
        }
        Replique replique = repliques.get(index);
        dire(replique.orateur(), replique.texte(), "Suivant ▶", () -> jouer(repliques, index + 1, fin));
    }

    /** Affiche un texte fixe, sans bouton. */
    public void dire(String qui, String quoi) {
        dire(qui, quoi, null, null);
    }

    /** Affiche un texte avec un bouton d'action. */
    public void dire(String qui, String quoi, String libelle, Runnable action) {
        orateur.setText(qui);
        orateur.setVisible(!qui.isEmpty());
        orateur.setManaged(!qui.isEmpty());
        texte.setText(quoi);
        suivant.setVisible(libelle != null);
        if (libelle != null) {
            suivant.setText(libelle);
            suivant.setOnAction(e -> action.run());
            suivant.requestFocus();
        }
    }
}
