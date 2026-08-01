# Stage 3 — ADB tap relay (laptop side)

This folder contains the laptop-side relay for Stage 3. The phone app does **not**
talk adb directly to the TV (a TV's USB port is a host port, so adb can never
travel over the phone↔TV USB cable). Instead:

```
[Phone app]  --HTTP POST /tap {x,y}-->  relay.py  --adb shell input tap x y-->  [TV]
```

The relay is the only thing that runs adb. It is Python 3 stdlib only — no pip
installs needed.

## 1. One-time TV setup

1. On the TV: Settings → System → About → press **Build** 7 times (enables
   Developer Options).
2. Developer options → enable **USB debugging** (and **Network debugging** /
   "ADB debugging over network" if your TV has it).
3. Put the TV and the laptop on the **same Wi-Fi**. Note the TV's IP
   (Settings → Network → status).

## 2. Start the relay

```
py -3 tools/relay.py --tv 192.168.1.50:5555
```

(or double-click `tools/relay.bat`). The relay runs `adb connect` to the TV —
accept the RSA fingerprint prompt on the TV the first time.

Check it worked: `adb devices` should list the TV (and the phone).

## 3. Link the phone to the laptop

USB (recommended — you'll already have the phone plugged in to install the APK):

```
adb reverse tcp:8080 tcp:8080
```

The app's Relay URL stays at `http://127.0.0.1:8080`.

Wi-Fi (alternative): set the app's Relay URL to `http://<laptop-ip>:8080`
(allow Python through the Windows Firewall if prompted).

## 4. Test

Open the app → **Send Test Tap** (defaults to the center of a 1920×1080 screen:
960,540). Watch the TV react at that spot.

Manual sanity check from the laptop:

```
adb -s 192.168.1.50:5555 shell input tap 960 540
```

If the tap lands off-target, get the TV's real resolution and update X/Y:

```
adb -s 192.168.1.50:5555 shell wm size
```
