import os, subprocess, urllib, requests, threading, time
from  http.server import HTTPServer, BaseHTTPRequestHandler

class HubRequestHandler(BaseHTTPRequestHandler):
    color0 = 2
    addrs = {"id0":"128.237.238.37"}

    def do_GET(self):
        if(self.path == ("/devices.json")):
           # Check that file exists
           # TODO: generate from SQL database
           try:
               devs = open("devices.json", "rb")

               # File exists, send OK response
               self.send_response(200)

               # Send headers
               self.send_header("Content-Type", "text/plain")
               self.send_header("Content-Length", os.path.getsize("devices.json"))
               self.end_headers()

               # Send the file
               self.wfile.write(devs.read())

               # Clean up
               devs.close()

           except FileNotFoundError:
               # Send a 404
               self.send_error(404)

        else:
            # If another file is requested, send a 404
            self.send_error(404)

    def do_POST(self):
        # We received a POST request from the app to tell us
        # to light certain devices

        # The request has the format
        # <id0>: <color0>
        # <id1>: <color1>
        #  ...
        #
        # Where <idN> is a device_id from our devices.json,
        # and <colorN> is a color specified as follows.
        #
        # Colors:
        # 0   - The hub can pick the color,
        #       but must be consistent within each request
        #       (so each 0 will have the same color).
        # 1-7 - Interpreted as a bitwise-OR between RED=0x1, GREEN=0x2, BLUE=0x4
        #       e.g. 1 = RED, 6 = CYAN, 7 = WHITE

        content_length = int(self.headers['content-length'])
        ids = urllib.parse.parse_qs(self.rfile.read(content_length),
                                    keep_blank_values = True)

        # If any devid isn't in addrs, send back a 404
        for devid in ids:
            if devid.decode() not in HubRequestHandler.addrs:
                print("Couldn't find device id ", devid)
                self.send_error(404)
                return

        # For each id, send a request to the relevant indicator.
        # We just use a GET request with the color we want to use
        for devid in ids:

            # Get color to request
            color = int(ids[devid][0])
            if(color == 0):
                color = HubRequestHandler.color0

            # Get addr from database
            addr = HubRequestHandler.addrs[devid.decode()]
            response = requests.get("http://" + addr + "/" + str(color))

            # If response isn't NO CONTENT (204) then we have a problem
            if(response.status_code != 204):
                print(response)
                raise IOError

        # TODO: thread the requests to the indicators for speeeeed

        # When we get an OK from each indicator, send OK to phone.
        # We'll tell them what color we assigned to 0
        asgn = ("{0:" + str(HubRequestHandler.color0) + "}").encode()
        self.send_response(200)
        self.send_header("Content-length", len(asgn))
        self.end_headers()
        self.wfile.write(asgn)

        # TODO: look at threading this
        time.sleep(5)

        # For each id, GET 0 (no color)
        for devid in ids:
            addr = HubRequestHandler.addrs[devid.decode()]
            response = requests.get("http://" + addr + "/0")

            # If response isn't NO CONTENT (204) then we have a problem
            if(response.status_code != 204):
                print(response)
                raise IOError

        # Change color0
        # Pick a color in [2,6] (i.e. not red or "white")
        HubRequestHandler.color0 = (HubRequestHandler.color0 + 1) % 5 + 2

try:
    # Advertise the server
    subprocess.Popen(["avahi-publish-service", "iot_hub", "_http._tcp.", "80"])
    print("avahi is advertising the hub")

    # Start the server
    server_address = ("", 80)
    httpd = HTTPServer(server_address, HubRequestHandler)
    httpd.serve_forever()

except KeyboardInterrupt:
    print("\nShutting down...")
    httpd.socket.close()
