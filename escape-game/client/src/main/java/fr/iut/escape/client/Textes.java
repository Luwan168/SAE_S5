package fr.iut.escape.client;

import fr.iut.escape.client.ui.Dialogue.Replique;

import java.util.List;

/** Tous les textes du scénario au même endroit, pour les modifier sans toucher aux écrans. */
public final class Textes {

    public static final String TITRE = "PRÉSUMÉ COUPABLE";
    public static final String SOUS_TITRE = "Un procès. Des preuves. Vos choix.";

    public static final String JUGE = "Le juge";
    public static final String AVOCAT = "L'avocat de la défense";
    public static final String TEMOIN = "Le témoin";

    /** Discours du juge, mission du joueur, puis témoignage. */
    public static List<Replique> tribunal(String pseudo) {
        return List.of(
                new Replique(JUGE, "Silence dans la salle ! L'audience est ouverte."),
                new Replique(JUGE, pseudo + ", vous comparaissez devant cette cour pour le meurtre "
                        + "de votre collègue, commis le soir du 14 mars."),
                new Replique(JUGE, "Les faits qui vous sont reprochés : votre badge a été utilisé ce soir-là, "
                        + "et un enregistrement vous montrerait sur les lieux vers 21 h."),
                new Replique("VOTRE MISSION", "Prouvez votre innocence. Trouvez les preuves, examinez-les "
                        + "et présentez à la cour ce qui contredit l'accusation. "
                        + "La façon dont vous les obtenez compte aussi."),
                new Replique(AVOCAT, "Monsieur le juge, les pièces originales existent. Elles sont sur une clé USB "
                        + "gardée dans la salle électrique, derrière un faisceau laser."),
                new Replique(AVOCAT, "Tant que le courant n'est pas coupé, personne ne peut s'en approcher. "
                        + "Et le boîtier de commande réclame un code que nous n'avons pas."),
                new Replique(TEMOIN, "J'ai tout mis par écrit le soir même, ma déposition est sur le bureau du juge. "
                        + "Lisez-la bien : les chiffres que j'y donne ne sont pas là par hasard."));
    }

    /** Témoignage écrit. Les passages entre ** ** sont affichés en gras. */
    public static final String TEMOIGNAGE =
            "Le 14 mars, vers **21h**, j'ai vu un homme sortir précipitamment de l'immeuble. "
            + "Il portait une veste sombre et semblait avoir couru sur au moins **35 mètres** avant de "
            + "s'arrêter net devant sa voiture, une berline grise immatriculée se terminant par **33**. "
            + "Il a jeté un sac dans le coffre — j'ai estimé son poids à environ **22 kg** — puis a démarré. "
            + "Le suspect est resté visible pendant **33 secondes** avant de disparaître au coin de la rue.";

    /** Grille jointe au témoignage : ligne puis colonne (21 = ligne 2, colonne 1 = A). */
    public static final String[][] GRILLE = {
            {"E", "N", "Q", "U", "T"},
            {"A", "B", "C", "D", "F"},
            {"G", "H", "I", "K", "L"},
            {"M", "O", "P", "R", "S"},
            {"V", "W", "X", "Y", "Z"}};

    private Textes() {
    }
}
