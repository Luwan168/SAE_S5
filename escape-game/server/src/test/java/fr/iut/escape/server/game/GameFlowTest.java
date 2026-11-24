package fr.iut.escape.server.game;

import com.jayway.jsonpath.JsonPath;
import fr.iut.escape.common.MqttTopics;
import fr.iut.escape.server.iot.LaserGateway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:test")
@AutoConfigureMockMvc
class GameFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    GameService service;
    @MockitoBean
    LaserGateway laser;

    private String newGame() throws Exception {
        String json = mvc.perform(post("/api/games").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pseudo\":\"Lukas\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.gameId");
    }

    private void action(String id, String evidence, String action, int expectedStatus) throws Exception {
        mvc.perform(post("/api/games/" + id + "/evidence").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"evidence\":\"" + evidence + "\",\"action\":\"" + action + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private void code(String id, String code, boolean accepted) throws Exception {
        mvc.perform(post("/api/games/" + id + "/power-code").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(jsonPath("$.accepted").value(accepted));
    }

    @Test
    void mauvaisCodeNeCoupePasLeCourant() throws Exception {
        String id = newGame();
        code(id, "AVOCAT", false);
        verify(laser, never()).powerOff();
        mvc.perform(post("/api/games/" + id + "/usb")).andExpect(status().isConflict());
    }

    @Test
    void parcoursHonneteMeneALAcquittement() throws Exception {
        String id = newGame();
        verify(laser).powerOn();

        service.laserMessage(MqttTopics.MSG_LASER_COUPE);
        code(id, " alibi ", true);
        verify(laser).powerOff();
        service.laserMessage(MqttTopics.MSG_SYSTEME_OFF);
        service.laserMessage(MqttTopics.MSG_LASER_COUPE);

        mvc.perform(get("/api/games/" + id))
                .andExpect(jsonPath("$.powerOff").value(true))
                .andExpect(jsonPath("$.iotConfirmed").value(true))
                .andExpect(jsonPath("$.alarms").value(1));

        mvc.perform(post("/api/games/" + id + "/usb")).andExpect(status().isOk());
        action(id, "preuve2", "PRESENT", 409);
        action(id, "preuve2", "ANALYZE", 200);
        action(id, "preuve2", "AI_DETECTOR", 200);
        action(id, "preuve2", "PRESENT", 200);

        mvc.perform(post("/api/games/" + id + "/verdict"))
                .andExpect(jsonPath("$.innocent").value(true))
                .andExpect(jsonPath("$.ethicsScore").value(2));
    }

    @Test
    void supprimerLesPreuvesMeneALaCondamnation() throws Exception {
        String id = newGame();
        code(id, "ALIBI", true);
        mvc.perform(post("/api/games/" + id + "/usb")).andExpect(status().isOk());
        action(id, "preuve1", "DELETE", 200);
        action(id, "preuve2", "DELETE", 200);
        action(id, "preuve2", "ANALYZE", 409);

        mvc.perform(post("/api/games/" + id + "/verdict"))
                .andExpect(jsonPath("$.innocent").value(false))
                .andExpect(jsonPath("$.ethicsScore").value(-2));
    }

    @AfterEach
    void horlogeNormale() {
        service.setClock(Clock.systemUTC());
    }

    private void aLaSeconde(Instant debut, int secondes) {
        service.setClock(Clock.fixed(debut.plusSeconds(secondes), ZoneOffset.UTC));
    }

    @Test
    void chronoEtPenaliteDuLaser() throws Exception {
        Instant debut = Instant.parse("2026-11-17T10:00:00Z");
        aLaSeconde(debut, 0);
        String id = newGame();
        mvc.perform(get("/api/games/" + id)).andExpect(jsonPath("$.remainingSeconds").value(1800));

        aLaSeconde(debut, 10);
        service.laserMessage(MqttTopics.MSG_LASER_COUPE);
        aLaSeconde(debut, 14);
        mvc.perform(get("/api/games/" + id))
                .andExpect(jsonPath("$.laserCut").value(true))
                .andExpect(jsonPath("$.remainingSeconds").value(1800 - 14 - 4 * 5));
        service.laserMessage(MqttTopics.MSG_LASER_RETABLI);

        aLaSeconde(debut, 100);
        mvc.perform(get("/api/games/" + id))
                .andExpect(jsonPath("$.laserCut").value(false))
                .andExpect(jsonPath("$.alarms").value(1))
                .andExpect(jsonPath("$.remainingSeconds").value(1800 - 100 - 20));

        aLaSeconde(debut, 1780);
        mvc.perform(get("/api/games/" + id))
                .andExpect(jsonPath("$.timeUp").value(true))
                .andExpect(jsonPath("$.remainingSeconds").value(0));
        mvc.perform(post("/api/games/" + id + "/power-code").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ALIBI\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/games/" + id + "/verdict"))
                .andExpect(jsonPath("$.innocent").value(false))
                .andExpect(jsonPath("$.timeUp").value(true));
    }

    @Test
    void leVerdictArreteLeChrono() throws Exception {
        Instant debut = Instant.parse("2026-11-17T10:00:00Z");
        aLaSeconde(debut, 0);
        String id = newGame();
        aLaSeconde(debut, 60);
        mvc.perform(post("/api/games/" + id + "/verdict")).andExpect(jsonPath("$.remainingSeconds").value(1740));
        aLaSeconde(debut, 600);
        mvc.perform(get("/api/games/" + id)).andExpect(jsonPath("$.remainingSeconds").value(1740));
        mvc.perform(post("/api/games/" + id + "/usb")).andExpect(status().isConflict());
    }

    @Test
    void partieInconnue() throws Exception {
        mvc.perform(get("/api/games/inexistante")).andExpect(status().isNotFound());
    }
}
