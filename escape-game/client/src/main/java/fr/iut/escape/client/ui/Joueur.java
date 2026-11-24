package fr.iut.escape.client.ui;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;

import java.util.function.Predicate;

/**
 * Le personnage du joueur, vu de dessus façon Pokémon.
 *
 * Pour changer de personnage : remplacer images/joueur.png par une planche de même disposition.
 *   - 4 lignes, une par direction, dans cet ordre : bas, gauche, droite, haut ;
 *   - sur chaque ligne, les images de la marche (la première sert aussi à l'arrêt) ;
 *   - toutes les cases ont la même taille : LARGEUR_CASE x HAUTEUR_CASE.
 * Si la nouvelle planche a des cases plus grandes ou plus d'images, il suffit d'ajuster les trois constantes.
 */
public final class Joueur {

    private static final String PLANCHE = "joueur.png";
    private static final int LARGEUR_CASE = 16;
    private static final int HAUTEUR_CASE = 20;
    private static final int IMAGES_PAR_DIRECTION = 2;

    /** Un pixel du personnage = 12 pixels du décor (les décors 3840x2160 sont dessinés en 320x180). */
    private static final double PIXEL = 12;
    /** Vitesse de marche, en pixels du décor par seconde. */
    private static final double VITESSE = 620;
    private static final double DUREE_IMAGE = 0.16;
    /** Seuls les pieds butent contre les obstacles : la tête peut passer devant un meuble. */
    private static final double LARGEUR_PIEDS = 108;
    private static final double HAUTEUR_PIEDS = 48;

    /** L'ordre de cette liste est celui des lignes de la planche. */
    private enum Direction { BAS, GAUCHE, DROITE, HAUT }

    private final ImageView vue;
    /** Position des pieds (milieu du bas du personnage), en pixels du décor. */
    private double x;
    private double y;
    private Direction direction = Direction.HAUT;
    private double tempsDeMarche;

    public Joueur(double x, double y) {
        this.x = x;
        this.y = y;
        vue = Ui.image(PLANCHE, LARGEUR_CASE * PIXEL * Ui.ECHELLE, HAUTEUR_CASE * PIXEL * Ui.ECHELLE);
        vue.setMouseTransparent(true);
        dessiner(false);
    }

    public ImageView vue() {
        return vue;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** Rectangle occupé par les pieds si le personnage était en (px, py). */
    public static Rectangle2D pieds(double px, double py) {
        return new Rectangle2D(px - LARGEUR_PIEDS / 2, py - HAUTEUR_PIEDS, LARGEUR_PIEDS, HAUTEUR_PIEDS);
    }

    /**
     * Avance dans la direction (dx, dy), chacun valant -1, 0 ou 1, pendant dt secondes.
     * Les deux axes sont testés séparément : contre un mur, le personnage glisse au lieu de se bloquer.
     */
    public void avancer(int dx, int dy, double dt, Predicate<Rectangle2D> libre) {
        boolean marche = dx != 0 || dy != 0;
        if (marche) {
            double pas = VITESSE * dt / (dx != 0 && dy != 0 ? Math.sqrt(2) : 1);
            if (dx != 0 && libre.test(pieds(x + dx * pas, y))) {
                x += dx * pas;
            }
            if (dy != 0 && libre.test(pieds(x, y + dy * pas))) {
                y += dy * pas;
            }
            direction = dy > 0 ? Direction.BAS : dy < 0 ? Direction.HAUT : dx < 0 ? Direction.GAUCHE : Direction.DROITE;
            tempsDeMarche += dt;
        } else {
            tempsDeMarche = 0;
        }
        dessiner(marche);
    }

    private void dessiner(boolean marche) {
        int image = marche ? 1 + (int) (tempsDeMarche / DUREE_IMAGE) : 0;
        vue.setViewport(new Rectangle2D((image % IMAGES_PAR_DIRECTION) * LARGEUR_CASE,
                direction.ordinal() * HAUTEUR_CASE, LARGEUR_CASE, HAUTEUR_CASE));
        vue.setLayoutX(x * Ui.ECHELLE - vue.getFitWidth() / 2);
        vue.setLayoutY(y * Ui.ECHELLE - vue.getFitHeight());
    }
}
