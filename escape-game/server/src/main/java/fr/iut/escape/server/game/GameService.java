package fr.iut.escape.server.game;

import fr.iut.escape.common.Api.EvidenceAction;
import fr.iut.escape.common.Api.GameStateResponse;
import fr.iut.escape.common.Api.VerdictResponse;
import fr.iut.escape.common.MqttTopics;
import fr.iut.escape.server.iot.LaserGateway;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Déroulé d'une partie : le serveur est la seule source de vérité, le client ne fait qu'afficher. */
@Service
public class GameService {

    private final GameRepository games;
    private final LaserGateway laser;
    private final String powerCode;
    private final int gameDuration;
    private final int laserPenalty;
    private Clock clock = Clock.systemUTC();

    /** Le module laser est unique : ses messages concernent la dernière partie lancée. */
    private volatile String activeGameId;

    public GameService(GameRepository games, LaserGateway laser,
                       @Value("${escape.power-code}") String powerCode,
                       @Value("${escape.duree-partie}") int gameDuration,
                       @Value("${escape.penalite-laser}") int laserPenalty) {
        this.games = games;
        this.laser = laser;
        this.powerCode = powerCode;
        this.gameDuration = gameDuration;
        this.laserPenalty = laserPenalty;
    }

    /** Pour les tests : remplacer l'horloge par une horloge fixe. */
    void setClock(Clock clock) {
        this.clock = clock;
    }

    @PostConstruct
    void listenToLaser() {
        laser.onLaserMessage(this::laserMessage);
    }

    public synchronized Game create(String pseudo) {
        String name = pseudo == null || pseudo.isBlank() ? "Accusé" : pseudo.trim();
        Game game = games.save(new Game(name, clock.instant(), gameDuration, laserPenalty));
        activeGameId = game.getId();
        laser.powerOn();
        return game;
    }

    public Game get(String id) {
        return games.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie inconnue"));
    }

    /** Bon code : on enregistre, puis on coupe le courant du laser via MQTT. */
    public synchronized boolean submitPowerCode(String id, String code) {
        Instant now = clock.instant();
        Game game = get(id);
        running(game, now);
        boolean accepted = code != null && powerCode.equalsIgnoreCase(code.trim());
        if (accepted) {
            game.cutPower(now);
            games.save(game);
            laser.powerOff();
        } else {
            game.wrongCode();
            games.save(game);
        }
        return accepted;
    }

    public synchronized Game insertUsb(String id) {
        return update(id, Game::insertUsb);
    }

    public synchronized Game evidence(String id, String evidence, EvidenceAction action) {
        if (evidence == null || action == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preuve et action obligatoires");
        }
        return update(id, game -> game.apply(evidence, action));
    }

    private Game update(String id, Consumer<Game> change) {
        Game game = get(id);
        running(game, clock.instant());
        try {
            change.accept(game);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        return games.save(game);
    }

    private static void running(Game game, Instant now) {
        try {
            game.requireRunning(now);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    /** Appelé à chaque message de la MKR sur escape/laser. */
    synchronized void laserMessage(String message) {
        String id = activeGameId;
        if (id == null) {
            return;
        }
        Instant now = clock.instant();
        games.findById(id).ifPresent(game -> {
            switch (message) {
                case MqttTopics.MSG_SYSTEME_OFF -> game.confirmPowerOff(now);
                case MqttTopics.MSG_LASER_COUPE -> game.laserCut(now);
                case MqttTopics.MSG_LASER_RETABLI -> game.laserRestored(now);
                default -> { }
            }
            games.save(game);
        });
    }

    public GameStateResponse state(Game g) {
        Instant now = clock.instant();
        long remaining = (long) Math.ceil(g.remainingMillis(now) / 1000.0);
        return new GameStateResponse(g.getId(), g.getPseudo(), g.isPowerOff(), g.isIotConfirmed(),
                laser.isConnected(), g.getAlarms(), g.isUsbInserted(), g.isEvidence1Deleted(),
                g.isEvidence2Deleted(), g.isEvidence2Analyzed(), g.isAiDetected(),
                g.isCounterEvidencePresented(), remaining, g.isLaserCut(), g.getLaserPenalty(),
                remaining == 0);
    }

    /**
     * Verdict du juge, en fonction des preuves trouvées et de ce que le joueur en a fait.
     * Demander le verdict termine la partie : le chrono s'arrête.
     */
    public synchronized VerdictResponse verdict(String id) {
        Instant now = clock.instant();
        Game g = get(id);
        g.end(now);
        games.save(g);
        long remaining = (long) Math.ceil(g.remainingMillis(now) / 1000.0);
        boolean timeUp = remaining == 0;

        String p = g.getPseudo();
        List<String> speech = new ArrayList<>();
        List<String> details = new ArrayList<>();
        boolean innocent = g.isCounterEvidencePresented();

        if (innocent) {
            speech.add("La défense a présenté une contre-preuve : l'enregistrement qui accablait "
                    + p + " a été entièrement généré par une IA.");
            speech.add("La pièce maîtresse de l'accusation est donc un faux. Le doute n'est plus permis.");
            if (g.isEvidence1Deleted()) {
                speech.add("La cour relève toutefois qu'une pièce du dossier a disparu de la clé. "
                        + "Vous êtes acquitté, " + p + ", mais cette destruction sera consignée.");
            } else {
                speech.add(p + ", vous êtes déclaré non coupable. Vous êtes libre.");
            }
        } else {
            if (timeUp) {
                speech.add("Le temps accordé à la défense est écoulé. L'audience reprend.");
            }
            speech.add("La cour a examiné les pièces qui lui ont été soumises.");
            if (g.isEvidence2Deleted()) {
                speech.add("Un enregistrement a été effacé de la clé avant d'être examiné. "
                        + "Faire disparaître une preuve ne prouve pas une innocence.");
            } else {
                speech.add("Rien n'est venu contredire l'enregistrement qui vous montre sur les lieux.");
            }
            speech.add(p + ", vous êtes déclaré coupable.");
        }

        if (g.isEvidence1Deleted()) {
            details.add("-1 : vous avez supprimé preuve1.jpg");
        }
        if (g.isEvidence2Deleted()) {
            details.add("-1 : vous avez supprimé preuve2.png");
        }
        if (g.isEvidence2Analyzed()) {
            details.add("+1 : vous avez analysé preuve2.png au lieu de la faire disparaître");
        }
        if (g.isCounterEvidencePresented()) {
            details.add("+1 : vous avez présenté la contre-preuve au juge");
        }
        if (details.isEmpty()) {
            details.add("Aucun choix enregistré sur les preuves.");
        }
        return new VerdictResponse(innocent, innocent ? "NON COUPABLE" : "COUPABLE",
                speech, g.getEthicsScore(), details, timeUp, remaining);
    }
}
