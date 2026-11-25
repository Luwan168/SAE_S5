// Module laser de l'Escape Game — Arduino MKR WiFi 1010 + MKR Connector Carrier (Grove)
//
// Branchements : LED verte D2, LED rouge D3, bouton ON/OFF D4, buzzer D5,
//                bouton « coupure » D6, écran LCD RGB sur TWI, capteur de lumière A0.
//
// MQTT (broker Mosquitto du Raspberry Pi) :
//   publie sur   escape/laser    : "systeme on" / "systeme off" / "laser coupe" / "laser retabli"
//   écoute       escape/commande : "on" / "off" / "toggle"  -> même effet que le bouton D4
// Le serveur du jeu envoie "off" quand le joueur saisit le bon code, et "on" au début d'une partie.
//
// Bibliothèques (gestionnaire de bibliothèques de l'IDE) : WiFiNINA, ArduinoMqttClient, Grove LCD RGB Backlight.
//
// Réglages : WIFI_SSID / WIFI_PASS = réseau commun au Pi, à la MKR et au PC ; BROKER = adresse du Pi.
// Ne pas nommer la constante du port « PORT » : c'est une macro du processeur SAMD21.
// SEUIL du capteur à calibrer : avec la lampe de test, au-dessus = coupure ;
// avec le vrai laser, remplacer « > SEUIL » par « < SEUIL ».

#include <WiFiNINA.h>
#include <ArduinoMqttClient.h>
#include <rgb_lcd.h>

const char WIFI_SSID[] = "Lukas";
const char WIFI_PASS[] = "TON_MOT_DE_PASSE";

const char BROKER[]    = "10.220.64.83";
const int  BROKER_PORT = 1883;
const char TOPIC[]     = "escape/laser";
const char TOPIC_CMD[] = "escape/commande";

const int PIN_LED_VERTE      = 2;
const int PIN_LED_ROUGE      = 3;
const int PIN_BOUTON_POWER   = 4;
const int PIN_BUZZER         = 5;
const int PIN_BOUTON_COUPURE = 6;
const int PIN_LUMIERE        = A0;
const int SEUIL = 500;

WiFiClient wifi;
MqttClient mqtt(wifi);
rgb_lcd lcd;

bool alimente = true;
bool coupe = false;
bool dernierPower = LOW;
unsigned long dernierEssai = 0;
String commandeRecue = "";

void connecterBroker() {
  if (mqtt.connected()) return;
  if (mqtt.connect(BROKER, BROKER_PORT)) {
    Serial.println("Broker connecte.");
    mqtt.subscribe(TOPIC_CMD);
  } else {
    Serial.print("Echec broker, code : ");
    Serial.println(mqtt.connectError());
  }
}

void envoyer(const char* message) {
  Serial.println(message);
  if (!mqtt.connected()) return;
  mqtt.beginMessage(TOPIC);
  mqtt.print(message);
  mqtt.endMessage();
}

void appliquerEtat() {
  lcd.clear();
  if (!alimente) {
    digitalWrite(PIN_LED_VERTE, LOW);
    digitalWrite(PIN_LED_ROUGE, LOW);
    noTone(PIN_BUZZER);
    lcd.setRGB(0, 0, 0);
    return;
  }
  digitalWrite(PIN_LED_VERTE, coupe ? LOW : HIGH);
  digitalWrite(PIN_LED_ROUGE, coupe ? HIGH : LOW);
  if (coupe) {
    tone(PIN_BUZZER, 1000);
    lcd.setRGB(255, 0, 0);
    lcd.print("Laser coupe");
  } else {
    noTone(PIN_BUZZER);
    lcd.setRGB(0, 255, 0);
    lcd.print("Laser active");
  }
}

void changerAlimentation(bool nouvelEtat) {
  if (nouvelEtat == alimente) return;
  alimente = nouvelEtat;
  if (!alimente && coupe) {
    coupe = false;
    envoyer("laser retabli");
  }
  envoyer(alimente ? "systeme on" : "systeme off");
  appliquerEtat();
}

void surMessage(int taille) {
  String msg = "";
  while (mqtt.available()) {
    msg += (char)mqtt.read();
  }
  msg.trim();
  commandeRecue = msg;
}

void setup() {
  Serial.begin(9600);
  while (!Serial && millis() < 3000);

  pinMode(PIN_LED_VERTE, OUTPUT);
  pinMode(PIN_LED_ROUGE, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_BOUTON_POWER, INPUT);
  pinMode(PIN_BOUTON_COUPURE, INPUT);
  lcd.begin(16, 2);
  appliquerEtat();

  Serial.print("Connexion au Wi-Fi");
  while (WiFi.begin(WIFI_SSID, WIFI_PASS) != WL_CONNECTED) {
    Serial.print(".");
    delay(2000);
  }
  Serial.println("\nWi-Fi connecte.");

  mqtt.onMessage(surMessage);
  connecterBroker();
}

void loop() {
  if (!mqtt.connected() && millis() - dernierEssai >= 5000) {
    dernierEssai = millis();
    connecterBroker();
  }
  mqtt.poll();

  if (commandeRecue != "") {
    if (commandeRecue == "off")         changerAlimentation(false);
    else if (commandeRecue == "on")     changerAlimentation(true);
    else if (commandeRecue == "toggle") changerAlimentation(!alimente);
    commandeRecue = "";
  }

  bool lecturePower = digitalRead(PIN_BOUTON_POWER);
  if (lecturePower == HIGH && dernierPower == LOW) {
    delay(50);
    changerAlimentation(!alimente);
  }
  dernierPower = lecturePower;

  if (alimente) {
    bool detection = digitalRead(PIN_BOUTON_COUPURE) == HIGH || analogRead(PIN_LUMIERE) > SEUIL;
    if (detection != coupe) {
      coupe = detection;
      envoyer(coupe ? "laser coupe" : "laser retabli");
      appliquerEtat();
    }
  }
  delay(20);
}
