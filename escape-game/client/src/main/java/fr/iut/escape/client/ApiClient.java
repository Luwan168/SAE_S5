package fr.iut.escape.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.iut.escape.common.Api.CreateGameRequest;
import fr.iut.escape.common.Api.EvidenceAction;
import fr.iut.escape.common.Api.EvidenceActionRequest;
import fr.iut.escape.common.Api.GameCreatedResponse;
import fr.iut.escape.common.Api.GameStateResponse;
import fr.iut.escape.common.Api.PowerCodeRequest;
import fr.iut.escape.common.Api.PowerCodeResponse;
import fr.iut.escape.common.Api.VerdictResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Appels REST vers le serveur. Méthodes bloquantes : à appeler via EscapeApp.appel(). */
public final class ApiClient {

    private final String base;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper json = new ObjectMapper();

    public ApiClient(String base) {
        this.base = base + "/api/games";
    }

    public GameCreatedResponse creerPartie(String pseudo) throws Exception {
        return envoyer("POST", "", new CreateGameRequest(pseudo), GameCreatedResponse.class);
    }

    public GameStateResponse etat(String id) throws Exception {
        return envoyer("GET", "/" + id, null, GameStateResponse.class);
    }

    public boolean saisirCode(String id, String code) throws Exception {
        return envoyer("POST", "/" + id + "/power-code", new PowerCodeRequest(code), PowerCodeResponse.class)
                .accepted();
    }

    public GameStateResponse cleBranchee(String id) throws Exception {
        return envoyer("POST", "/" + id + "/usb", null, GameStateResponse.class);
    }

    public GameStateResponse preuve(String id, String preuve, EvidenceAction action) throws Exception {
        return envoyer("POST", "/" + id + "/evidence", new EvidenceActionRequest(preuve, action),
                GameStateResponse.class);
    }

    public VerdictResponse verdict(String id) throws Exception {
        return envoyer("POST", "/" + id + "/verdict", null, VerdictResponse.class);
    }

    private <T> T envoyer(String methode, String chemin, Object corps, Class<T> type) throws Exception {
        HttpRequest.BodyPublisher contenu = corps == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corps));
        HttpRequest requete = HttpRequest.newBuilder(URI.create(base + chemin))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .method(methode, contenu)
                .build();
        HttpResponse<String> reponse;
        try {
            reponse = http.send(requete, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IOException("Serveur injoignable (" + Config.SERVEUR + ")", e);
        }
        if (reponse.statusCode() >= 300) {
            JsonNode erreur = json.readTree(reponse.body());
            throw new IOException(erreur.path("message").asText("Erreur " + reponse.statusCode()));
        }
        return json.readValue(reponse.body(), type);
    }
}
