#!/usr/bin/env python3
"""
Stage 3 relay — bridges the phone app to `adb shell input tap` on your TV.

Run it on the laptop (the only machine that needs adb):

    py -3 tools/relay.py --tv 192.168.1.50:5555

It connects to the TV once (accept the RSA prompt on the TV the first time),
then listens on port 8080. The phone app POSTs a tap here and the relay runs
the adb command against the TV:

    Phone app --HTTP POST /tap {x,y}--> relay.py --adb shell input tap x y--> TV

Phone -> laptop links (either works):
  USB  (recommended): plug the phone into the laptop, then run:
                      adb reverse tcp:8080 tcp:8080
                      and keep the app's Relay URL at http://127.0.0.1:8080
  Wi-Fi:              set the app's Relay URL to http://<laptop-ip>:8080
                      (allow Python through the Windows Firewall if prompted)

Requires Python 3 (no extra packages - stdlib only). Find the TV's IP in the
TV's Settings -> Network.
"""
import argparse
import json
import os
import shutil
import subprocess
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DEFAULT_TAP_X = 960
DEFAULT_TAP_Y = 540


def find_adb(adb_arg):
    """Locate the adb binary (PATH first, then the common platform-tools path)."""
    if adb_arg and os.path.isfile(adb_arg):
        return adb_arg
    if adb_arg and shutil.which(adb_arg):
        return adb_arg
    win_default = os.path.join(os.environ.get("LOCALAPPDATA", ""),
                               "Android", "platform-tools", "adb.exe")
    if os.path.isfile(win_default):
        return win_default
    return adb_arg


def run(cmd):
    """Run a command; return (returncode, stdout, stderr)."""
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
        return proc.returncode, proc.stdout.strip(), proc.stderr.strip()
    except Exception as exc:
        return 1, "", str(exc)


def to_int(value, fallback):
    """Parse a tap coordinate defensively so a bad payload can never crash a request."""
    try:
        return int(value)
    except (TypeError, ValueError):
        return fallback


def main():
    parser = argparse.ArgumentParser(description="Stage 3 adb tap relay")
    parser.add_argument("--tv", default="",
                        help="TV adb address, e.g. 192.168.1.50:5555")
    parser.add_argument("--port", type=int, default=8080,
                        help="HTTP port to listen on (default 8080)")
    parser.add_argument("--adb", default="adb",
                        help="path to adb (default: adb from PATH or platform-tools)")
    args = parser.parse_args()

    adb = find_adb(args.adb)
    tv = args.tv

    if not tv:
        print("[relay] WARNING: no --tv given, so every tap will fail.")
        print("[relay]         Restart with: py -3 tools/relay.py --tv <TV_IP>:5555")

    if tv:
        code, out, err = run([adb, "connect", tv])
        print("[relay] adb connect %s -> %s"
              % (tv, out or err or ("exit code %d" % code)))

    print("[relay] adb = %s" % adb)
    print("[relay] listening on 0.0.0.0:%d - TV target: %s"
          % (args.port, tv or "NOT SET"))
    print("[relay] phone->laptop over USB: adb reverse tcp:%d tcp:%d"
          % (args.port, args.port))
    print("[relay] phone->laptop over Wi-Fi: Relay URL = http://<laptop-ip>:%d"
          % args.port)
    print("[relay] quick test: curl -X POST http://127.0.0.1:%d/tap "
          "-H \"Content-Type: application/json\" -d \"{\\\"x\\\":960,\\\"y\\\":540}\""
          % args.port)

    class Handler(BaseHTTPRequestHandler):
        def do_POST(self):
            length = int(self.headers.get("Content-Length") or 0)
            raw = self.rfile.read(length) if length else b"{}"
            try:
                data = json.loads(raw.decode("utf-8") or "{}")
            except Exception:
                data = {}
            x = to_int(data.get("x"), DEFAULT_TAP_X)
            y = to_int(data.get("y"), DEFAULT_TAP_Y)
            if not tv:
                self._json(500, {"ok": False, "error": "relay started without --tv"})
                return
            code, out, err = run([adb, "-s", tv, "shell", "input", "tap",
                                  str(x), str(y)])
            if code != 0:
                # The TV may have dropped; try reconnecting once before failing.
                run([adb, "connect", tv])
                code, out, err = run([adb, "-s", tv, "shell", "input", "tap",
                                      str(x), str(y)])
            print("[relay] tap %d,%d -> %s" % (x, y, out or err or ("adb exit %d" % code)))
            if code == 0:
                self._json(200, {"ok": True, "x": x, "y": y, "output": out})
            else:
                self._json(502, {"ok": False, "error": err or ("adb exit %d" % code)})

        def do_GET(self):
            if self.path == "/status":
                code, out, err = run([adb, "devices"])
                self._json(200, {"ok": True, "tv": tv, "adb_devices": out or err})
            else:
                self._json(200, {"ok": True, "tv": tv,
                                 "tip": "POST /tap with {\"x\":..,\"y\":..}"})

        def _json(self, status, obj):
            data = json.dumps(obj).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

        def log_message(self, fmt, *args):
            print("[relay]", fmt % args)

    ThreadingHTTPServer(("0.0.0.0", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
