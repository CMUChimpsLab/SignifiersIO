#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>

const char* ssid = "CMU";
const char* password = "whatever";
const char* mDNSName = "indicator0";

const int ledR = 13;
const int ledG = 12;
const int ledB = 14;

const char RED_MASK = 1;
const char GREEN_MASK = 2;
const char BLUE_MASK = 4;

// Whether the server has received a request yet. Flash the IP until it has.
bool pinged = false;
IPAddress ip;

ESP8266WebServer server(80);

// Turn the LED to color
void light(char color) {
  digitalWrite(ledR, color & RED_MASK);
  digitalWrite(ledG, color & GREEN_MASK);
  digitalWrite(ledB, color & BLUE_MASK);
}

void handleAll() {
  pinged = true;

  // Get the color from the URI
  String colorURI = server.uri();
  char color = colorURI.charAt(1) - 48;
  Serial.print("Color: ");
  Serial.println((int)color);

  if (0 <= color && color <= 7) {
    // If it's a valid color, light up and send the response
    light(color);
    server.send(204);

  } else {
    String message = "Invalid color\n\n";
    message += "URI: ";
    message += server.uri();
    message += "\nMethod: ";
    message += (server.method() == HTTP_GET) ? "GET" : "POST";
    message += "\nArguments: ";
    message += server.args();
    message += "\n";
    for (uint8_t i = 0; i < server.args(); i++) {
      message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
    }
    server.send(404, "text/plain", message);
  }
}

void setup(void) {
  pinMode(ledR, OUTPUT);
  pinMode(ledG, OUTPUT);
  pinMode(ledB, OUTPUT);

  Serial.begin(115200);
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  Serial.println("");
  Serial.print("MAC ");
  Serial.println(WiFi.macAddress());

  // Wait for connection
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  ip = WiFi.localIP();
  Serial.println("");
  Serial.print("Connected to ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(ip);

  if (MDNS.begin(mDNSName)) {
    Serial.println("MDNS responder started");
  }

  server.on("/", []() {
    server.send(200, "text/plain", "GET /1 through /7 to turn on the lights");
  });
  server.onNotFound(handleAll);

  server.begin();
  Serial.println("HTTP server started");
}

void loop(void) {
  server.handleClient();
  if (!pinged) {
    flashIP();
  }
}

const long tbit = 500; // ms to flash one bit
const long tdiv = 20; // ms to flicker between bits
/*
  BBBBr    r    rBBBBrrrrrrBBBB....rrrrrrGGGGGGGGGGGGGGGGGGGGrrrrr
  bit.dbit.dbit.dbit.dbit.dbit.....dbit.dbit.dbit.dbit.dbit.dbit.d
  \________9_________/\___/\_#_..._/\___/\________._________/\___/
*/

long ipstart = -100000; // Start of flashing
const long period = 5 * (tbit + tdiv);

// Light up the led to show the ip
void flashIP() {
  long curt = millis() - ipstart;

  if (curt > (ip.toString().length() + 3) * period) {
    // Restart
    light(0);
    ipstart = millis();
    
  } else if (curt > ip.toString().length() * period) {
    // Long green pulse at the end
    light(GREEN_MASK);

  } else if ((curt % period ) < (4 * (tbit + tdiv))) {
    // We're outputting a character right now
    int chari = curt / period;           // Index into string
    char c = ip.toString().charAt(chari);  // char we're outputting

    if (c == '.') {
      // We're outputting '.'
      light(GREEN_MASK);

    } else {
      // We're outputting a digit

      if ((curt % (tbit + tdiv)) > tbit) {
        // We're outputting a divider
        light(RED_MASK);

      } else {
        // We're outputting a bit
        char outc = c - 48; // Number we're outputting
        int biti = (curt % period) / (tbit + tdiv);

        // 0 if the biti'th-highest bit is a 0, >0 otherwise
        int out = outc & (0x1 << (3 - biti));
        if (out == 0) {
          light(0);
        } else {
          light(BLUE_MASK);
        }
      }
    }

  } else {
    // We're outputting a gap between digits
    light(RED_MASK);

  }
}
