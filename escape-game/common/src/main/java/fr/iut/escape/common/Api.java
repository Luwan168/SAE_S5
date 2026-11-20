package fr.iut.escape.common;

import java.util.List;

/** Objets échangés en JSON entre le client JavaFX et le serveur. */
public final class Api {

    /** Identifiants des deux fichiers de la clé USB. */
    public static final String PREUVE_1 = "preuve1";
    public static final String PREUVE_2 = "preuve2";

    /** Ce que le joueur peut faire d'une preuve. */
    public enum EvidenceAction {
        /** « Supprimer la preuve » */
        DELETE,
        /** « Analyser la preuve » */
        ANALYZE,
        /** « Passer au détecteur d'IA » */
        AI_DETECTOR,
        /** « Présenter cette contre-preuve » */
        PRESENT
    }

    /** Corps de POST /api/games. */
    public record CreateGameRequest(String pseudo) {
    }

    /** Réponse de POST /api/games. */
    public record GameCreatedResponse(String gameId, String pseudo) {
    }

    /** Corps de POST /api/games/{id}/power-code. */
    public record PowerCodeRequest(String code) {
    }

    /** Réponse de POST /api/games/{id}/power-code. */
    public record PowerCodeResponse(boolean accepted) {
    }

    /** Corps de POST /api/games/{id}/evidence. */
    public record EvidenceActionRequest(String evidence, EvidenceAction action) {
    }

    /**
     * État complet d'une partie (GET /api/games/{id}).
     *
     * @param powerOff         le bon code a été saisi, l'ordre « off » est parti vers la carte
     * @param iotConfirmed     la carte a répondu « systeme off »
     * @param brokerConnected  le serveur est relié au broker MQTT
     * @param alarms           nombre de fois où le faisceau a été coupé alors que le courant passait
     * @param remainingSeconds temps restant au chrono, pénalités comprises
     * @param laserCut         le faisceau est coupé en ce moment : le chrono s'accélère
     * @param laserPenalty     secondes retirées en plus par seconde de coupure
     * @param timeUp           temps écoulé : le client passe au verdict
     */
    public record GameStateResponse(
            String gameId,
            String pseudo,
            boolean powerOff,
            boolean iotConfirmed,
            boolean brokerConnected,
            int alarms,
            boolean usbInserted,
            boolean evidence1Deleted,
            boolean evidence2Deleted,
            boolean evidence2Analyzed,
            boolean aiDetected,
            boolean counterEvidencePresented,
            long remainingSeconds,
            boolean laserCut,
            int laserPenalty,
            boolean timeUp) {
    }

    /** Réponse de POST /api/games/{id}/verdict (qui arrête le chrono). */
    public record VerdictResponse(
            boolean innocent,
            String title,
            List<String> judgeSpeech,
            int ethicsScore,
            List<String> ethicsDetails,
            boolean timeUp,
            long remainingSeconds) {
    }

    private Api() {
    }
}
