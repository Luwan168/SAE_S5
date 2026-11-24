package fr.iut.escape.client.ui;

import fr.iut.escape.client.Textes;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** Le document du témoin : sa déposition à gauche, la grille à droite. */
public final class VueDocument {

    public static StackPane creer(String libelleBouton, Runnable action) {
        TextFlow temoignage = new TextFlow();
        temoignage.setPrefWidth(430);
        temoignage.setLineSpacing(4);
        String[] morceaux = Textes.TEMOIGNAGE.split("\\*\\*");
        for (int i = 0; i < morceaux.length; i++) {
            Text morceau = new Text(morceaux[i]);
            morceau.getStyleClass().add(i % 2 == 1 ? "papier-gras" : "papier");
            temoignage.getChildren().add(morceau);
        }

        GridPane grille = new GridPane();
        grille.getStyleClass().add("grille");
        for (int i = 0; i <= 5; i++) {
            for (int j = 0; j <= 5; j++) {
                String contenu = i == 0 && j == 0 ? "" : i == 0 ? "" + j : j == 0 ? "" + i : Textes.GRILLE[i - 1][j - 1];
                Label caseGrille = new Label(contenu);
                caseGrille.getStyleClass().add(i == 0 || j == 0 ? "case-entete" : "case");
                grille.add(caseGrille, j, i);
            }
        }

        HBox feuille = new HBox(30, temoignage, grille);
        feuille.getStyleClass().add("feuille");
        feuille.setAlignment(Pos.CENTER);
        VBox contenu = new VBox(14, Ui.texte("DÉPOSITION DU TÉMOIN", "titre-popup"), feuille);
        contenu.setAlignment(Pos.CENTER);
        return Ui.popup(900, contenu, Ui.bouton(libelleBouton, action));
    }

    private VueDocument() {
    }
}
