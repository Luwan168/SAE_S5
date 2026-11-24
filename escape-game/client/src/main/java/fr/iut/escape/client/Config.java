package fr.iut.escape.client;

/** Réglages du client, modifiables au lancement avec -Dnom=valeur. */
public final class Config {

    /** Adresse du serveur Spring Boot : -Descape.serveur=http://192.168.1.20:8080 */
    public static final String SERVEUR = System.getProperty("escape.serveur", "http://localhost:8080");

    /** Durée des deux chargements (téléchargement de la clé, détecteur d'IA), en secondes : -Descape.duree=5 */
    public static final int DUREE_CHARGEMENT = Integer.getInteger("escape.duree", 30);

    /**
     * Dossier supplémentaire à surveiller pour la clé USB (optionnel) : -Descape.usb.dossier=C:\test
     * Créer un sous-dossier dedans équivaut à brancher une clé ; pratique pour tester sans clé.
     */
    public static final String DOSSIER_USB = System.getProperty("escape.usb.dossier");

    /** Affiche les obstacles des salles en rouge par-dessus le décor : -Descape.debug=true */
    public static final boolean DEBUG = Boolean.getBoolean("escape.debug");

    /**
     * Mode démo : plein écran et toujours au premier plan : -Descape.kiosque=true
     * La fenêtre de l'Explorateur qui s'ouvre au branchement de la clé reste alors derrière le jeu.
     */
    public static final boolean KIOSQUE = Boolean.getBoolean("escape.kiosque");

    private Config() {
    }
}
