package fr.iut.escape.server.iot;

import java.util.function.Consumer;

/** Ce que le jeu sait faire du module laser, sans connaître MQTT. */
public interface LaserGateway {

    /** Coupe le courant du laser : équivalent d'un appui sur le premier bouton (D4). */
    boolean powerOff();

    /** Réarme le laser en début de partie. */
    boolean powerOn();

    /** Vrai si le serveur est relié au broker. */
    boolean isConnected();

    /** Reçoit chaque message publié par la MKR sur escape/laser. */
    void onLaserMessage(Consumer<String> listener);
}
