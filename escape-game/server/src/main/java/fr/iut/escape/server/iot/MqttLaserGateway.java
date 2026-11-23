package fr.iut.escape.server.iot;

import fr.iut.escape.common.MqttTopics;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Liaison MQTT avec le broker Mosquitto du Raspberry Pi.
 * Le serveur démarre même si le broker est absent : la connexion est retentée toutes les 5 secondes.
 */
@Component
public class MqttLaserGateway implements LaserGateway {

    private static final Logger log = LoggerFactory.getLogger(MqttLaserGateway.class);

    private final String url;
    private final ScheduledExecutorService retry = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mqtt-connexion");
        t.setDaemon(true);
        return t;
    });
    private volatile Consumer<String> listener = message -> { };
    private MqttClient client;

    public MqttLaserGateway(@Value("${escape.mqtt.url}") String url) {
        this.url = url;
    }

    @PostConstruct
    void start() throws MqttException {
        client = new MqttClient(url, "escape-serveur-" + UUID.randomUUID(), new MemoryPersistence());
        retry.scheduleWithFixedDelay(this::connectIfNeeded, 0, 5, TimeUnit.SECONDS);
    }

    private void connectIfNeeded() {
        if (client.isConnected()) {
            return;
        }
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(4);
            client.connect(options);
            client.subscribe(MqttTopics.LASER, 1, (topic, message) -> {
                String text = new String(message.getPayload(), StandardCharsets.UTF_8).trim();
                log.info("[{}] {}", topic, text);
                listener.accept(text);
            });
            log.info("Connecté au broker MQTT {}", url);
        } catch (MqttException e) {
            log.warn("Broker MQTT {} injoignable ({}), nouvel essai dans 5 s", url, e.getMessage());
        }
    }

    private boolean publish(String command) {
        try {
            client.publish(MqttTopics.COMMANDE, command.getBytes(StandardCharsets.UTF_8), 1, false);
            log.info("[{}] {}", MqttTopics.COMMANDE, command);
            return true;
        } catch (MqttException e) {
            log.warn("Ordre « {} » non envoyé : {}", command, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean powerOff() {
        return publish(MqttTopics.CMD_OFF);
    }

    @Override
    public boolean powerOn() {
        return publish(MqttTopics.CMD_ON);
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public void onLaserMessage(Consumer<String> listener) {
        this.listener = listener;
    }

    @PreDestroy
    void stop() {
        retry.shutdownNow();
        try {
            if (client.isConnected()) {
                client.disconnect(1000);
            }
            client.close();
        } catch (MqttException ignored) {
        }
    }
}
