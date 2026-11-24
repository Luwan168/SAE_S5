package fr.iut.escape.client;

import javafx.application.Platform;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Détecte le branchement d'une clé USB sans bibliothèque externe : on compare, chaque seconde,
 * la liste des volumes montés à celle de la seconde précédente.
 * Windows : une nouvelle lettre de lecteur (E:\). Linux : /media/<user>/... macOS : /Volumes/...
 * Une clé déjà branchée au démarrage n'est pas comptée : il faut la brancher après la coupure du courant.
 */
public final class DetecteurUsb {

    private final ScheduledExecutorService veille = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "detecteur-usb");
        t.setDaemon(true);
        return t;
    });
    private Set<String> connus;

    /** @param surBranchement appelé une seule fois, sur le fil JavaFX, avec le chemin de la clé */
    public void demarrer(java.util.function.Consumer<String> surBranchement) {
        connus = volumes();
        veille.scheduleWithFixedDelay(() -> {
            Set<String> actuels = volumes();
            for (String volume : actuels) {
                if (!connus.contains(volume)) {
                    veille.shutdown();
                    Platform.runLater(() -> surBranchement.accept(volume));
                    return;
                }
            }
            connus = actuels;
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void arreter() {
        veille.shutdownNow();
    }

    private static Set<String> volumes() {
        Set<String> resultat = new HashSet<>();
        for (File racine : File.listRoots()) {
            resultat.add(racine.getAbsolutePath());
        }
        String utilisateur = System.getProperty("user.name");
        String[] dossiers = {"/media/" + utilisateur, "/run/media/" + utilisateur, "/media", "/Volumes",
                Config.DOSSIER_USB};
        for (String dossier : dossiers) {
            File[] contenu = dossier == null ? null : new File(dossier).listFiles(File::isDirectory);
            if (contenu != null) {
                for (File volume : contenu) {
                    resultat.add(volume.getAbsolutePath());
                }
            }
        }
        return resultat;
    }
}
