package fr.iut.escape.server.game;

import fr.iut.escape.common.Api.CreateGameRequest;
import fr.iut.escape.common.Api.EvidenceActionRequest;
import fr.iut.escape.common.Api.GameCreatedResponse;
import fr.iut.escape.common.Api.GameStateResponse;
import fr.iut.escape.common.Api.PowerCodeRequest;
import fr.iut.escape.common.Api.PowerCodeResponse;
import fr.iut.escape.common.Api.VerdictResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService service;

    public GameController(GameService service) {
        this.service = service;
    }

    /** Nouvelle partie (page de saisie du pseudo). */
    @PostMapping
    public GameCreatedResponse create(@RequestBody CreateGameRequest request) {
        Game game = service.create(request.pseudo());
        return new GameCreatedResponse(game.getId(), game.getPseudo());
    }

    /** État de la partie ; le client l'interroge chaque seconde dans la salle électrique. */
    @GetMapping("/{id}")
    public GameStateResponse state(@PathVariable String id) {
        return service.state(service.get(id));
    }

    /** Code saisi sur le boîtier : s'il est bon, le serveur coupe le courant du laser. */
    @PostMapping("/{id}/power-code")
    public PowerCodeResponse powerCode(@PathVariable String id, @RequestBody PowerCodeRequest request) {
        return new PowerCodeResponse(service.submitPowerCode(id, request.code()));
    }

    /** La clé USB vient d'être branchée sur le PC du joueur. */
    @PostMapping("/{id}/usb")
    public GameStateResponse usb(@PathVariable String id) {
        return service.state(service.insertUsb(id));
    }

    /** Supprimer / analyser / passer au détecteur / présenter une preuve. */
    @PostMapping("/{id}/evidence")
    public GameStateResponse evidence(@PathVariable String id, @RequestBody EvidenceActionRequest request) {
        return service.state(service.evidence(id, request.evidence(), request.action()));
    }

    /** Rend le verdict et arrête le chrono. */
    @PostMapping("/{id}/verdict")
    public VerdictResponse verdict(@PathVariable String id) {
        return service.verdict(id);
    }
}
