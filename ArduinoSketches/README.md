This directory contains Arduino sketches to run on the ESP8266.
Each indicator LED is controlled by one ESP8266.

Arduino IDE settings:
  Board: "Generic ESP8266 Module"
  Flash Mode: "DIO"
  Flash Size: 4M (1M SPIFFS)"
  Debug port: "Disabled"
  Debug Level: "None"
  IwIP Variant: "v2 Lower Memory"
  Reset Method: "none"
  Crystal Frequency: "26 MHz"
  Flash Frequency: "40 MHz"
  CPU Frequency: "80 MHz"
  Builtin Led: "2"
  Upload Speed: "115200"
  Erase Flash: "Only Sketch"
  Programmer: "AVRISP mkII"

I'm using a USB to serial board with a FTDI chip (which provides Serial RX and TX).

Board configuration:

+++++++++++++++++++++++++++++++++++++++++++ 3.3 V
                                  | | |
             ___________          | | |
            |  ESP8266  |         | | |
            |           |         | | |
 Serial RX--|TXD0    RST|--10kOhm-- | |
 Serial TX--|RXD0    ADC|           | |
            |IO5      EN|--10kOhm---- |
            |IO4    IO16|             |
----10kOhm--|IO0    IO14|             |
|           |IO2    IO12|             |
| --10kOhm--|IO15   IO13|             |
| |       --|GND     VCC|--------------
| |       | |___________|
| |       |
------------------------------------------- 0 V

This will boot the ESP8266 in programming mode.
To reset it, connect RST to 0V.
To put it in run mode, connect IO0 to 3.3V *INSTEAD OF* 0V and reset.
After uploading a sketch, the ESP8266 will automatically reboot into run mode. To upload a new sketch, reset it.

Resources:
https://tttapa.github.io/ESP8266/Chap01%20-%20ESP8266.html
https://github.com/esp8266/Arduino/tree/master/doc