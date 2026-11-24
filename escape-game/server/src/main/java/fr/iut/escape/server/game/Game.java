package fr.iut.escape.server.game;

import fr.iut.escape.common.Api;
import fr.iut.escape.common.Api.EvidenceAction;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Une partie et les choix du joueur. Les règles du jeu sont ici : chrono, pénalités du laser,
 * énigme de la clé USB.
 * Les méthodes qui dépendent du temps reçoivent l'instant présent en paramètre, ce qui permet de les tester.
 */
@Entity
@Table(name = "games")
public class Game {

    @Id
    private String id;
    private String pseudo;
    private Instant startedAt;

    private Integer durationSeconds;
    /** Secondes retirées en plus du temps normal, pour chaque seconde où le faisceau est coupé. */
    private Integer laserPenalty;
    /** Pénalités déjà acquises (coupures terminées), en millisecondes. */
    private Long penaltyMillis;
    /** Début de la coupure en cours, ou null si le faisceau est intact. */
    private Instant laserCutSince;
    /** Fin de partie (verdict demandé) : le chrono est figé à cet instant. */
    private Instant endedAt;

    private boolean powerOff;
    private boolean iotConfirmed;
    private int wrongCodes;
    private int alarms;
    private boolean usbInserted;

    private boolean evidence1Deleted;
    private boolean evidence2Deleted;
    private boolean evidence2Analyzed;
    private boolean aiDetected;
    private boolean counterEvidencePresented;
    private int ethicsScore;

    protected Game() {
    }

    public Game(String pseudo, Instant now, int durationSeconds, int laserPenalty) {
        this.id = UUID.randomUUID().toString();
        this.pseudo = pseudo;
        this.startedAt = now;
        this.durationSeconds = durationSeconds;
        this.laserPenalty = laserPenalty;
        this.penaltyMillis = 0L;
    }

    /** Temps restant : durée - temps écoulé - pénalités (y compris la coupure en cours). */
    public long remainingMillis(Instant now) {
        Instant t = endedAt != null ? endedAt : now;
        long penalty = penaltyMillis();
        if (laserCutSince != null) {
            penalty += Duration.between(laserCutSince, t).toMillis() * laserPenalty();
        }
        long elapsed = Duration.between(startedAt, t).toMillis();
        return Math.max(0, durationSeconds() * 1000L - elapsed - penalty);
    }

    public boolean isTimeUp(Instant now) {
        return remainingMillis(now) == 0;
    }

    /** Refuse toute action une fois le temps écoulé ou le verdict rendu. */
    public void requireRunning(Instant now) {
        require(endedAt == null, "La partie est terminée.");
        require(!isTimeUp(now), "Le temps est écoulé.");
    }

    /** Verdict demandé : le chrono s'arrête. */
    public void end(Instant now) {
        if (endedAt == null) {
            endLaserCut(now);
            endedAt = now;
        }
    }

    public void cutPower(Instant now) {
        powerOff = true;
        endLaserCut(now);
    }

    public void wrongCode() {
        wrongCodes++;
    }

    /** La MKR a répondu « systeme off » : le laser n'a plus de courant. */
    public void confirmPowerOff(Instant now) {
        if (powerOff) {
            iotConfirmed = true;
        }
        endLaserCut(now);
    }

    /** Faisceau coupé : alarme et début de pénalité, seulement si le courant est encore là. */
    public void laserCut(Instant now) {
        if (powerOff || endedAt != null || laserCutSince != null) {
            return;
        }
        alarms++;
        laserCutSince = now;
    }

    public void laserRestored(Instant now) {
        endLaserCut(now);
    }

    private void endLaserCut(Instant now) {
        if (laserCutSince != null) {
            penaltyMillis = penaltyMillis() + Duration.between(laserCutSince, now).toMillis() * laserPenalty();
            laserCutSince = null;
        }
    }

    public void insertUsb() {
        require(powerOff, "Le courant n'est pas coupé : la clé est encore derrière le laser.");
        usbInserted = true;
    }

    public void apply(String evidence, EvidenceAction action) {
        require(usbInserted, "La clé USB n'est pas branchée.");
        if (Api.PREUVE_1.equals(evidence) && action == EvidenceAction.DELETE) {
            if (!evidence1Deleted) {
                evidence1Deleted = true;
                ethicsScore--;
            }
            return;
        }
        require(Api.PREUVE_2.equals(evidence), "Action impossible sur cette preuve.");
        require(!evidence2Deleted, "Cette preuve a été supprimée.");
        switch (action) {
            case DELETE -> {
                evidence2Deleted = true;
                ethicsScore--;
            }
            case ANALYZE -> {
                if (!evidence2Analyzed) {
                    evidence2Analyzed = true;
                    ethicsScore++;
                }
            }
            case AI_DETECTOR -> {
                require(evidence2Analyzed, "Il faut d'abord analyser la preuve.");
                aiDetected = true;
            }
            case PRESENT -> {
                require(aiDetected, "Le détecteur d'IA n'a pas encore rendu son résultat.");
                if (!counterEvidencePresented) {
                    counterEvidencePresented = true;
                    ethicsScore++;
                }
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private int durationSeconds() { return durationSeconds == null ? 0 : durationSeconds; }
    private int laserPenalty() { return laserPenalty == null ? 0 : laserPenalty; }
    private long penaltyMillis() { return penaltyMillis == null ? 0 : penaltyMillis; }

    public String getId() { return id; }
    public String getPseudo() { return pseudo; }
    public Instant getStartedAt() { return startedAt; }
    public int getLaserPenalty() { return laserPenalty(); }
    public boolean isLaserCut() { return laserCutSince != null; }
    public boolean isEnded() { return endedAt != null; }
    public boolean isPowerOff() { return powerOff; }
    public boolean isIotConfirmed() { return iotConfirmed; }
    public int getWrongCodes() { return wrongCodes; }
    public int getAlarms() { return alarms; }
    public boolean isUsbInserted() { return usbInserted; }
    public boolean isEvidence1Deleted() { return evidence1Deleted; }
    public boolean isEvidence2Deleted() { return evidence2Deleted; }
    public boolean isEvidence2Analyzed() { return evidence2Analyzed; }
    public boolean isAiDetected() { return aiDetected; }
    public boolean isCounterEvidencePresented() { return counterEvidencePresented; }
    public int getEthicsScore() { return ethicsScore; }
}
