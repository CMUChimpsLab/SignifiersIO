#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>

const char* ssid = "Max's iPhone";
const char* password = "indicatorhub";
const char* mDNSName = "indicator1";

ESP8266WebServer server(80);

const int ledR = 13;
const int ledG = 12;
const int ledB = 14;

const char RED_MASK = 1;
const char GREEN_MASK = 2;
const char BLUE_MASK = 4;

void handleAll() {
  // Get the color from the URI
  String colorURI = server.uri();
  char color = colorURI.charAt(1) - 48;
  Serial.print("Color: ");
  Serial.println((int)color);

  if (0 <= color && color <= 7) {
    // If it's a valid color, light up and send the response
    digitalWrite(ledR, color & RED_MASK);
    digitalWrite(ledG, color & GREEN_MASK);
    digitalWrite(ledB, color & BLUE_MASK);

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
  Serial.println("");
  Serial.print("Connected to ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());

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
}
