package fr.iut.escape.client.ui;

import fr.iut.escape.client.Config;
import fr.iut.escape.client.ui.Dialogue.Replique;
import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Une salle dans laquelle le joueur se déplace (flèches, ZQSD ou WASD) et interagit (E, Entrée, Espace).
 *
 * Le décor est une simple image : tout ce qui est « solide » ou « cliquable » est décrit ici par des
 * rectangles invisibles posés par-dessus, en pixels de l'image (3840x2160) :
 *   - sol(...)         : la zone où l'on peut marcher ;
 *   - obstacle(...)    : un meuble ou un mur que le joueur ne traverse pas ;
 *   - interaction(...) : un objet utilisable quand le joueur est juste à côté.
 * Lancer le client avec -Descape.debug=true affiche ces rectangles par-dessus le décor.
 */
public abstract class Salle extends Ecran {

    /** Distance maximale (pixels du décor) entre les pieds du joueur et un objet pour pouvoir l'utiliser. */
    private static final double PORTEE = 260;

    /** Un objet du décor que le joueur peut utiliser en s'approchant. */
    public final class Interaction {
        private final Region zone;
        private final Rectangle2D rectangle;
        private final String invite;
        private final Runnable action;
        private boolean active = true;

        private Interaction(Rectangle2D rectangle, String invite, Runnable action) {
            this.rectangle = rectangle;
            this.invite = invite;
            this.action = action;
            zone = Ui.zone(rectangle.getMinX(), rectangle.getMinY(), rectangle.getWidth(), rectangle.getHeight(),
                    this::clic);
            zone.getStyleClass().add("zone-active");
        }

        /** Un objet inactif est invisible et inutilisable (ex. la feuille pendant le dialogue). */
        public void activer(boolean oui) {
            active = oui;
            zone.setVisible(oui);
        }

        private void clic() {
            if (blocages > 0) {
                return;
            }
            if (proche == this) {
                action.run();
            } else {
                indice("Approchez-vous pour faire cela.");
            }
        }

        private boolean aPortee() {
            double dx = Math.max(0, Math.max(rectangle.getMinX() - joueur.x(), joueur.x() - rectangle.getMaxX()));
            double dy = Math.max(0, Math.max(rectangle.getMinY() - joueur.y(), joueur.y() - rectangle.getMaxY()));
            return active && Math.hypot(dx, dy) <= PORTEE;
        }
    }

    protected final Joueur joueur;
    private final Dialogue dialogue = new Dialogue();
    private final Label bandeau = Ui.texte("", "indice");
    private final Pane calqueDebug = new Pane();

    private Rectangle2D sol = new Rectangle2D(0, 0, 3840, 2160);
    private final List<Rectangle2D> obstacles = new ArrayList<>();
    private final Map<Rectangle2D, String> nomsObstacles = new HashMap<>();
    private final List<Interaction> interactions = new ArrayList<>();
    private final Set<KeyCode> touches = EnumSet.noneOf(KeyCode.class);

    /** Nombre de popups ou dialogues ouverts : tant qu'il y en a, le joueur ne bouge pas. */
    private int blocages;
    private Interaction proche;
    private String indice = "";
    private long dernierInstant;

    private final AnimationTimer boucle = new AnimationTimer() {
        @Override
        public void handle(long maintenant) {
            double dt = dernierInstant == 0 ? 0 : Math.min((maintenant - dernierInstant) / 1e9, 0.05);
            dernierInstant = maintenant;
            if (blocages == 0) {
                image(dt);
            }
        }
    };

    private final EventHandler<KeyEvent> clavier = this::touche;

    protected Salle(String decor, double departX, double departY) {
        super(decor);
        joueur = new Joueur(departX, departY);
        calqueDebug.setMouseTransparent(true);
        bandeau.setMouseTransparent(true);
        bandeau.setLayoutX(40);
        bandeau.setLayoutY(668);
        bandeau.setPrefWidth(Ui.W - 80);
        bandeau.setVisible(false);
        dialogue.setVisible(false);
        ajouter(calqueDebug, joueur.vue(), bandeau, dialogue);

        sceneProperty().addListener((obs, ancienne, nouvelle) -> {
            if (ancienne != null) {
                ancienne.removeEventFilter(KeyEvent.ANY, clavier);
            }
            if (nouvelle != null) {
                nouvelle.addEventFilter(KeyEvent.ANY, clavier);
            }
        });
        boucle.start();
    }

    protected void sol(double x, double y, double largeur, double hauteur) {
        sol = new Rectangle2D(x, y, largeur, hauteur);
    }

    /** Le nom ne sert qu'à se repérer : il s'affiche sur le rectangle en mode -Descape.debug=true. */
    protected Rectangle2D obstacle(String nom, double x, double y, double largeur, double hauteur) {
        Rectangle2D rectangle = new Rectangle2D(x, y, largeur, hauteur);
        obstacles.add(rectangle);
        nomsObstacles.put(rectangle, nom);
        dessinerDebug();
        return rectangle;
    }

    protected void retirerObstacle(Rectangle2D rectangle) {
        obstacles.remove(rectangle);
        nomsObstacles.remove(rectangle);
        dessinerDebug();
    }

    protected Interaction interaction(double x, double y, double largeur, double hauteur, String invite, Runnable action) {
        Interaction objet = new Interaction(new Rectangle2D(x, y, largeur, hauteur), invite, action);
        interactions.add(objet);
        ajouterSousJoueur(objet.zone);
        return objet;
    }

    /** Ajoute un élément de décor animé (lasers, pénombre...) : au-dessus de l'image, sous le personnage. */
    protected void ajouterSousJoueur(Node... noeuds) {
        getChildren().addAll(getChildren().indexOf(calqueDebug), List.of(noeuds));
    }

    /** Texte d'aide affiché sur une ligne, en bas de l'écran. */
    protected void indice(String texte) {
        indice = texte;
        afficherBandeau();
    }

    private void afficherBandeau() {
        String texte = proche != null ? proche.invite : indice;
        bandeau.setText(texte);
        bandeau.setVisible(!texte.isEmpty() && blocages == 0);
    }

    /** Joue un dialogue ; le joueur ne peut ni bouger ni interagir avant la fin. */
    protected void dialogue(List<Replique> repliques, Runnable fin) {
        bloquer();
        dialogue.setVisible(true);
        dialogue.jouer(repliques, () -> {
            dialogue.setVisible(false);
            debloquer();
            fin.run();
        });
    }

    /** Ouvre une fenêtre par-dessus la salle et fige le joueur. */
    protected void ouvrir(Node popup) {
        bloquer();
        ajouter(popup);
    }

    protected void fermer(Node popup) {
        retirer(popup);
        debloquer();
    }

    private void bloquer() {
        blocages++;
        touches.clear();
        joueur.avancer(0, 0, 0, this::libre);
        afficherBandeau();
    }

    private void debloquer() {
        blocages--;
        afficherBandeau();
    }

    private void image(double dt) {
        int dx = (appuyee(KeyCode.RIGHT, KeyCode.D) ? 1 : 0) - (appuyee(KeyCode.LEFT, KeyCode.Q, KeyCode.A) ? 1 : 0);
        int dy = (appuyee(KeyCode.DOWN, KeyCode.S) ? 1 : 0) - (appuyee(KeyCode.UP, KeyCode.Z, KeyCode.W) ? 1 : 0);
        joueur.avancer(dx, dy, dt, this::libre);

        Interaction trouvee = null;
        for (Interaction objet : interactions) {
            if (objet.aPortee()) {
                trouvee = objet;
                break;
            }
        }
        if (trouvee != proche) {
            if (proche != null) {
                proche.zone.getStyleClass().remove("zone-proche");
            }
            if (trouvee != null) {
                trouvee.zone.getStyleClass().add("zone-proche");
            }
            proche = trouvee;
            afficherBandeau();
        }
        if (dx != 0 || dy != 0) {
            apresDeplacement();
        }
    }

    /** Appelé après chaque pas : les salles s'en servent pour leurs sorties. */
    protected void apresDeplacement() {
    }

    private boolean libre(Rectangle2D pieds) {
        if (!sol.contains(pieds)) {
            return false;
        }
        for (Rectangle2D obstacle : obstacles) {
            if (obstacle.intersects(pieds)) {
                return false;
            }
        }
        return true;
    }

    private boolean appuyee(KeyCode... codes) {
        for (KeyCode code : codes) {
            if (touches.contains(code)) {
                return true;
            }
        }
        return false;
    }

    private void touche(KeyEvent evenement) {
        if (blocages > 0) {
            return;
        }
        KeyCode code = evenement.getCode();
        boolean deplacement = switch (code) {
            case UP, DOWN, LEFT, RIGHT, Z, Q, S, D, W, A -> true;
            default -> false;
        };
        boolean action = code == KeyCode.E || code == KeyCode.ENTER || code == KeyCode.SPACE;
        if (!deplacement && !action) {
            return;
        }
        evenement.consume();
        if (evenement.getEventType() == KeyEvent.KEY_PRESSED) {
            if (deplacement) {
                touches.add(code);
            } else if (proche != null) {
                proche.action.run();
            }
        } else if (evenement.getEventType() == KeyEvent.KEY_RELEASED) {
            touches.remove(code);
        }
    }

    private void dessinerDebug() {
        if (!Config.DEBUG) {
            return;
        }
        calqueDebug.getChildren().clear();
        for (Rectangle2D o : obstacles) {
            Label rectangle = new Label(nomsObstacles.get(o));
            rectangle.getStyleClass().add("debug-obstacle");
            rectangle.setLayoutX(o.getMinX() * Ui.ECHELLE);
            rectangle.setLayoutY(o.getMinY() * Ui.ECHELLE);
            rectangle.setPrefSize(o.getWidth() * Ui.ECHELLE, o.getHeight() * Ui.ECHELLE);
            calqueDebug.getChildren().add(rectangle);
        }
    }

    @Override
    public void quitter() {
        boucle.stop();
    }
}
