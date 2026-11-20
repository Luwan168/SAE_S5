package fr.iut.escape.common;

/**
 * Contrat MQTT entre le serveur et la carte MKR WiFi 1010 (module laser).
 * Ces valeurs doivent rester identiques à celles du croquis iot/laser_mkr/laser_mkr.ino.
 */
public final class MqttTopics {

    /** La MKR publie ici son état. */
    public static final String LASER = "escape/laser";
    /** Le serveur publie ici ses ordres ; la MKR réagit comme au bouton ON/OFF (D4). */
    public static final String COMMANDE = "escape/commande";

    public static final String CMD_ON = "on";
    public static final String CMD_OFF = "off";

    public static final String MSG_LASER_COUPE = "laser coupe";
    public static final String MSG_LASER_RETABLI = "laser retabli";
    public static final String MSG_SYSTEME_ON = "systeme on";
    public static final String MSG_SYSTEME_OFF = "systeme off";

    private MqttTopics() {
    }
}
