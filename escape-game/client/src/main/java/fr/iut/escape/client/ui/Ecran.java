package fr.iut.escape.client.ui;

import fr.iut.escape.common.Api.GameStateResponse;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/** Un écran du jeu, dessiné en 1280x720 par-dessus un décor. */
public abstract class Ecran extends Pane {

    protected Ecran(String decor) {
        setPrefSize(Ui.W, Ui.H);
        getChildren().add(Ui.image(decor, Ui.W, Ui.H));
    }

    protected void ajouter(Node... noeuds) {
        getChildren().addAll(noeuds);
    }

    protected void retirer(Node noeud) {
        getChildren().remove(noeud);
    }

    /** État de la partie reçu du serveur (deux fois par seconde pendant une partie). */
    public void etatRecu(GameStateResponse etat) {
    }

    /** Appelé quand on change d'écran : arrêter ici les animations et les tâches de fond. */
    public void quitter() {
    }
}
