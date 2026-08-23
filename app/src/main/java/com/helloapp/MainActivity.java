package com.helloapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.content.SharedPreferences;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stages 2, 3 and 4.
 *
 * Stage 2 - USB connection detection:
 *   Detects when the phone is connected to another device (e.g. an Android Smart TV)
 *   over a USB-C cable. If the phone is the USB HOST it can read the connected
 *   device's manufacturer/product name; in peripheral mode it can only report that
 *   a connection exists; with nothing connected it shows a safe default state.
 *
 * Stage 3 - ADB test tap (via laptop relay - KEPT as a fallback):
 *   The phone app sends one HTTP POST to a small relay that runs on your laptop
 *   (tools/relay.py). The relay executes `adb shell input tap x y` against the TV
 *   over Wi-Fi. The phone itself never talks adb to the TV - a TV's USB port is a
 *   host port, so adb cannot travel over the phone<->TV USB cable.
 *
 * Stage 4 - Embedded adb client (the final, laptop-free mode):
 *   The phone itself is the adb client. It connects straight to the TV's adbd over
 *   TCP (port 5555) using the com.tananaev:adblib library (pure-Java implementation
 *   of the ADB wire protocol, no root needed). The only requirement at runtime is
 *   that the phone and the TV are on the same Wi-Fi network - e.g. the phone's own
 *   mobile hotspot with the TV joined to it; no internet is needed.
 *
 *   First-connection trust: the phone generates an RSA key pair on first use and
 *   stores it in the app's private files dir (like adb's ~/.android/adbkey). During
 *   connect() the adbd on the TV challenges the phone with a random token (AUTH),
 *   the phone signs it, and - because the TV has never seen this key before - the
 *   TV shows its normal "Allow USB debugging?" RSA fingerprint dialog ON THE TV
 *   SCREEN. Accept it with the TV remote and the TV saves the key; from then on the
 *   connection is silent. So yes: the TV shows the same prompt it shows for a laptop.
 */
public class MainActivity extends AppCompatActivity {

    /** Brand keywords suggesting a connected USB device is a TV / display. */
    private static final String[] TV_BRAND_KEYWORDS = {
            "samsung", "lg", "sony", "bravia", "hisense", "tcl", "philips",
            "panasonic", "vizio", "xiaomi", "sharp", "toshiba", "insignia",
            "skyworth", "haier", "roku", "onn", "westinghouse"
    };

    private static final int MAX_EVENT_LOG_LINES = 30;

    // Stage 3 defaults (matches the relay defaults).
    private static final String DEFAULT_RELAY_URL = "http://127.0.0.1:8080";
    private static final int DEFAULT_TAP_X = 960;
    private static final int DEFAULT_TAP_Y = 540;
    private static final int HTTP_TIMEOUT_MS = 5000;

    // Stage 4 defaults.
    private static final int DEFAULT_ADB_PORT = 5555;
    /** How long to wait for the TV to accept our TCP connection. */
    private static final int TCP_CONNECT_TIMEOUT_MS = 5000;
    /** How long to wait for the full AUTH handshake (incl. the user accepting the
     *  "Allow USB debugging?" dialog on the TV screen). */
    private static final int AUTH_WAIT_TIMEOUT_MS = 45000;
    /** Bounds a single tap command so a dead TV can never hang the UI forever. */
    private static final int COMMAND_TIMEOUT_MS = 10000;
    /** Fixed test tap coordinates (center of a 1920x1080 screen), as specified. */
    private static final int STAGE4_TAP_X = 960;
    private static final int STAGE4_TAP_Y = 540;
    /** adb key pair files kept in the app's private storage so the TV only asks
     *  for authorization once (mirrors adb's ~/.android/adbkey). */
    private static final String ADB_PRIVATE_KEY_FILE = "adbkey";
    private static final String ADB_PUBLIC_KEY_FILE = "adbkey.pub";
    private static final String PREFS_NAME = "stage4_prefs";
    private static final String PREFS_KEY_TV_IP = "tv_ip";
    private static final String PREFS_KEY_TV_PORT = "tv_port";

    // ------------------------------------------------------------------
    // USB state broadcast strings.
    //
    // The system sends a sticky USB_STATE broadcast
    // ("android.hardware.usb.action.USB_STATE") with boolean extras
    // "connected" and "configured" whenever the USB connection state changes.
    // UsbManager exposes ACTION_USB_STATE / USB_CONNECTED / USB_CONFIGURED
    // only as @SystemApi (@hide) constants, so they are NOT part of the public
    // SDK and cannot be referenced from an app. These string literals are
    // byte-for-byte identical to the hidden constants' values, so runtime
    // behavior is unchanged.
    // ------------------------------------------------------------------
    private static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    private static final String EXTRA_USB_CONNECTED = "connected";
    private static final String EXTRA_USB_CONFIGURED = "configured";

    private UsbManager usbManager;

    private View statusDot;
    private TextView statusTitle;
    private TextView tvName;
    private TextView tvDescription;
    private TextView detailsText;

    private EditText relayUrlInput;
    private EditText tapXInput;
    private EditText tapYInput;
    private TextView tapResultText;

    // Stage 4 views / state.
    private EditText tvIpInput;
    private EditText tvPortInput;
    private Button connectButton;
    private Button directTapButton;
    private TextView stage4StatusText;
    /** Live adb connection to the TV (only non-null while connected). */
    private AdbConnection adbConnection;
    private boolean stage4Connecting;
    private boolean stage4Connected;

    private final StringBuilder eventLog = new StringBuilder();

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                logEvent("Broadcast: " + action);
            }
            refreshUsbState();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        statusDot = findViewById(R.id.statusDot);
        statusTitle = findViewById(R.id.statusTitle);
        tvName = findViewById(R.id.tvName);
        tvDescription = findViewById(R.id.tvDescription);
        detailsText = findViewById(R.id.detailsText);

        relayUrlInput = findViewById(R.id.relayUrlInput);
        tapXInput = findViewById(R.id.tapXInput);
        tapYInput = findViewById(R.id.tapYInput);
        tapResultText = findViewById(R.id.tapResultText);

        // Stage 4 views.
        tvIpInput = findViewById(R.id.tvIpInput);
        tvPortInput = findViewById(R.id.tvPortInput);
        connectButton = findViewById(R.id.connectButton);
        directTapButton = findViewById(R.id.directTapButton);
        stage4StatusText = findViewById(R.id.stage4StatusText);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        tvIpInput.setText(prefs.getString(PREFS_KEY_TV_IP, ""));
        tvPortInput.setText(prefs.getString(PREFS_KEY_TV_PORT, String.valueOf(DEFAULT_ADB_PORT)));
        updateStage4Buttons();

        Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logEvent("Manual refresh");
                refreshUsbState();
            }
        });

        Button helloButton = findViewById(R.id.helloButton);
        helloButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, R.string.hello_toast, Toast.LENGTH_SHORT).show();
            }
        });

        Button sendTapButton = findViewById(R.id.sendTapButton);
        sendTapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendTestTap();
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectDirect();
            }
        });

        directTapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDirectTap();
            }
        });

        Button backHomeButton = findViewById(R.id.btnBackHome);
        backHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to HomeActivity
            }
        });

        // Initial scan — shows a safe default state when nothing is connected.
        refreshUsbState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_STATE);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        ContextCompat.registerReceiver(this, usbReceiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        refreshUsbState();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(usbReceiver);
        } catch (IllegalArgumentException ignored) {
            // Receiver was never registered; nothing to do.
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Tear down the adb socket so no worker thread outlives the activity.
        closeDirectConnection();
    }

    // ------------------------------------------------------------------
    // Stage 3: send one test tap through the laptop relay (tools/relay.py)
    // ------------------------------------------------------------------

    private void sendTestTap() {
        String baseUrl = relayUrlInput.getText().toString().trim();
        if (baseUrl.isEmpty()) {
            baseUrl = DEFAULT_RELAY_URL;
            relayUrlInput.setText(baseUrl);
        }

        final int x = parseCoord(tapXInput, DEFAULT_TAP_X);
        final int y = parseCoord(tapYInput, DEFAULT_TAP_Y);
        tapXInput.setText(String.valueOf(x));
        tapYInput.setText(String.valueOf(y));

        final String url = baseUrl.endsWith("/") ? baseUrl + "tap" : baseUrl + "/tap";
        final String body = "{\"x\":" + x + ",\"y\":" + y + "}";

        tapResultText.setText(getString(R.string.tap_sending, url));

        // HTTP must never run on the UI thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = httpPost(url, body);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tapResultText.setText(result);
                    }
                });
            }
        }).start();
    }

    private static int parseCoord(EditText input, int fallback) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String httpPost(String urlString, String body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(HTTP_TIMEOUT_MS);
            conn.setReadTimeout(HTTP_TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();

            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = in == null ? "" : readStream(in);
            return "HTTP " + code + " - " + response.trim();
        } catch (Exception e) {
            return "Could not reach relay: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readStream(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            sb.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Stage 4: embedded adb client - phone talks directly to the TV's adbd.
    // ------------------------------------------------------------------

    /**
     * Loads the app's adb RSA key pair, generating and persisting it on first use.
     * The TV only needs to authorize this key once; afterwards the handshake is
     * silent. The files live in getFilesDir()/adbkey(.pub) - app-private, no
     * permissions needed.
     */
    private AdbCrypto loadOrCreateAdbCrypto() throws IOException, GeneralSecurityException {
        AdbBase64 base64 = new AdbBase64() {
            @Override
            public String encodeToString(byte[] data) {
                return Base64.encodeToString(data, Base64.NO_WRAP);
            }
        };
        File priv = new File(getFilesDir(), ADB_PRIVATE_KEY_FILE);
        File pub = new File(getFilesDir(), ADB_PUBLIC_KEY_FILE);
        if (priv.exists() && pub.exists()) {
            return AdbCrypto.loadAdbKeyPair(base64, priv, pub);
        }
        AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(base64);
        crypto.saveAdbKeyPair(priv, pub);
        return crypto;
    }

    /** Connect button handler: TCP + RSA handshake with the TV's adbd. */
    private void connectDirect() {
        if (stage4Connecting) {
            return; // already working
        }
        final String ip = tvIpInput.getText().toString().trim();
        final int port = parsePort(tvPortInput, DEFAULT_ADB_PORT);
        tvPortInput.setText(String.valueOf(port));

        if (ip.isEmpty()) {
            setStage4Status(getString(R.string.stage4_error_no_ip));
            return;
        }

        // Remember the last address the user tried.
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(PREFS_KEY_TV_IP, ip)
                .putString(PREFS_KEY_TV_PORT, String.valueOf(port))
                .apply();

        closeDirectConnection();
        stage4Connecting = true;
        updateStage4Buttons();
        logEvent("Stage 4: connecting to " + ip + ":" + port + " ...");
        setStage4Status(getString(R.string.stage4_status_connecting, ip, port));

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Stage4ConnectResult result = new Stage4ConnectResult();
                Socket socket = null;
                AdbConnection conn = null;
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), TCP_CONNECT_TIMEOUT_MS);
                    AdbCrypto crypto = loadOrCreateAdbCrypto();
                    conn = AdbConnection.create(socket, crypto);
                    // throwOnUnauthorised=false: the first connection is expected to
                    // be "rejected" while the TV shows its Allow dialog; connect()
                    // keeps waiting until the user accepts (or the timeout hits).
                    boolean connected = conn.connect(AUTH_WAIT_TIMEOUT_MS,
                            TimeUnit.MILLISECONDS, false);
                    if (connected) {
                        result.success = true;
                    } else {
                        result.message = getString(R.string.stage4_error_not_authorized_timeout);
                    }
                } catch (UnknownHostException e) {
                    result.message = getString(R.string.stage4_error_bad_ip, ip);
                } catch (SocketTimeoutException | ConnectException e) {
                    result.message = getString(R.string.stage4_error_unreachable, ip, port);
                } catch (IOException e) {
                    // TCP reached the TV but the handshake dropped - almost always
                    // because the TV refused/closed before authorizing this key.
                    result.message = getString(R.string.stage4_error_not_authorized_closed);
                } catch (GeneralSecurityException e) {
                    result.message = getString(R.string.stage4_error_keygen);
                } catch (Exception e) {
                    result.message = getString(R.string.stage4_error_generic, e.getMessage());
                }

                final AdbConnection finishedConn = conn;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isDestroyed() || isFinishing()) {
                            // The activity went away mid-connect; do not leak the socket.
                            closeQuietly(finishedConn);
                            return;
                        }
                        stage4Connecting = false;
                        if (result.success) {
                            adbConnection = finishedConn;
                            stage4Connected = true;
                            logEvent("Stage 4: connected to " + ip + ":" + port);
                            setStage4Status(getString(R.string.stage4_status_connected, ip, port));
                        } else {
                            closeQuietly(finishedConn);
                            stage4Connected = false;
                            logEvent("Stage 4: " + result.message);
                            setStage4Status(result.message);
                        }
                        updateStage4Buttons();
                    }
                });
            }
        }).start();
    }

    /** Send Test Tap button handler: runs `input tap 960 540` on the TV. */
    private void sendDirectTap() {
        // Capture the connection up front so the worker thread never races the UI
        // thread (e.g. a fresh Connect() nulling the field mid-command).
        final AdbConnection conn = adbConnection;
        if (conn == null || !stage4Connected) {
            setStage4Status(getString(R.string.stage4_error_not_connected));
            return;
        }
        logEvent("Stage 4: sending tap " + STAGE4_TAP_X + "," + STAGE4_TAP_Y);
        setStage4Status(getString(R.string.stage4_status_tapping, STAGE4_TAP_X, STAGE4_TAP_Y));

        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder output = new StringBuilder();
                final AtomicBoolean watchdogFired = new AtomicBoolean(false);
                String error = null;

                // Watchdog: if the TV dies mid-command, closing the connection is
                // what unblocks the open()/read() calls below (adblib has no
                // per-call timeout) - otherwise the tap would hang forever.
                final Thread watchdog = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(COMMAND_TIMEOUT_MS);
                        } catch (InterruptedException e) {
                            return; // command finished in time
                        }
                        watchdogFired.set(true);
                        closeQuietly(conn);
                    }
                });
                watchdog.setDaemon(true);
                watchdog.start();

                try {
                    AdbStream stream = conn.open(
                            "shell:input tap " + STAGE4_TAP_X + " " + STAGE4_TAP_Y);
                    // Drain until adbd closes the shell stream (it closes it when
                    // the command finishes). 'input tap' prints nothing on success.
                    while (true) {
                        output.append(new String(stream.read(), StandardCharsets.UTF_8));
                    }
                } catch (IOException streamEnded) {
                    // Normal end of stream / command finished.
                } catch (Exception e) {
                    error = e.getMessage();
                } finally {
                    watchdog.interrupt();
                }

                final String err = error;
                final boolean timedOut = watchdogFired.get();
                final String out = output.toString().trim();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isDestroyed() || isFinishing()) {
                            return;
                        }
                        if (timedOut) {
                            logEvent("Stage 4: tap timed out after " + COMMAND_TIMEOUT_MS + " ms");
                            closeDirectConnection();
                            setStage4Status(getString(R.string.stage4_status_tap_timeout));
                            updateStage4Buttons();
                        } else if (err == null) {
                            if (out.isEmpty()) {
                                logEvent("Stage 4: tap sent (no output = success)");
                                setStage4Status(getString(R.string.stage4_status_tap_ok,
                                        STAGE4_TAP_X, STAGE4_TAP_Y));
                            } else {
                                logEvent("Stage 4: tap output: " + out);
                                setStage4Status(getString(R.string.stage4_status_tap_output, out));
                            }
                        } else {
                            // The connection almost certainly died - drop it so the
                            // user reconnects cleanly.
                            logEvent("Stage 4: tap failed: " + err);
                            closeDirectConnection();
                            setStage4Status(getString(R.string.stage4_status_tap_failed, err));
                            updateStage4Buttons();
                        }
                    }
                });
            }
        }).start();
    }

    private void closeDirectConnection() {
        if (adbConnection != null) {
            closeQuietly(adbConnection);
            adbConnection = null;
        }
        stage4Connected = false;
        if (connectButton != null && directTapButton != null) {
            updateStage4Buttons();
        }
    }

    private static void closeQuietly(AdbConnection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (IOException ignored) {
                // Already gone.
            }
        }
    }

    private void updateStage4Buttons() {
        connectButton.setEnabled(!stage4Connecting);
        directTapButton.setEnabled(stage4Connected && !stage4Connecting);
    }

    private void setStage4Status(String text) {
        stage4StatusText.setText(text);
    }

    private static int parsePort(EditText input, int fallback) {
        try {
            int p = Integer.parseInt(input.getText().toString().trim());
            return (p > 0 && p < 65536) ? p : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Small holder so the worker thread can pass back a result to the UI thread. */
    private static class Stage4ConnectResult {
        boolean success;
        String message = "";
    }

    // ------------------------------------------------------------------
    // Stage 2: USB connection state
    // ------------------------------------------------------------------

    private void refreshUsbState() {
        if (usbManager == null) {
            setStatus(R.color.status_default, getString(R.string.status_title_default),
                    getString(R.string.tv_name_default),
                    "USB service is unavailable on this device.");
            return;
        }

        // 1) Host-mode devices (phone is the USB host and can read device info).
        HashMap<String, UsbDevice> hostDevices = new HashMap<>();
        try {
            hostDevices.putAll(usbManager.getDeviceList());
        } catch (Exception e) {
            logEvent("getDeviceList() error: " + e.getMessage());
        }

        // 2) Accessories (Android Open Accessory protocol).
        UsbAccessory[] accessories = null;
        try {
            accessories = usbManager.getAccessoryList();
        } catch (Exception e) {
            logEvent("getAccessoryList() error: " + e.getMessage());
        }

        // 3) General USB connection state (fires in both host and peripheral modes).
        boolean usbConnected = false;
        boolean usbConfigured = false;
        String stateExtras = "none";
        try {
            // registerReceiver with a null receiver synchronously returns the last
            // sticky ACTION_USB_STATE broadcast without registering a listener.
            Intent state = registerReceiver(null, new IntentFilter(ACTION_USB_STATE));
            if (state != null) {
                usbConnected = state.getBooleanExtra(EXTRA_USB_CONNECTED, false);
                usbConfigured = state.getBooleanExtra(EXTRA_USB_CONFIGURED, false);
                stateExtras = dumpUsbStateExtras(state);
            }
        } catch (Exception e) {
            logEvent("USB state read error: " + e.getMessage());
        }

        // Build the technical details panel.
        StringBuilder detail = new StringBuilder();
        detail.append("=== USB CONNECTION STATE ===\n");
        detail.append("Phone role: ");
        if (!hostDevices.isEmpty()) {
            detail.append("HOST (phone is driving the connection)\n");
        } else if (usbConnected) {
            detail.append("PERIPHERAL (phone is the USB device - e.g. plugged into a TV)\n");
        } else {
            detail.append("no active USB connection\n");
        }
        detail.append("USB connected (sticky): ").append(usbConnected).append('\n');
        detail.append("USB configured (sticky): ").append(usbConfigured).append('\n');
        detail.append("Sticky extras: ").append(stateExtras).append("\n\n");

        detail.append("=== HOST-MODE DEVICES (").append(hostDevices.size()).append(") ===\n");
        if (hostDevices.isEmpty()) {
            detail.append("(none - the phone is not the USB host right now)\n");
        }
        UsbDevice tvCandidate = null;
        for (UsbDevice device : hostDevices.values()) {
            detail.append(describeDevice(device));
            if (looksLikeTv(device)) {
                tvCandidate = device;
            }
        }
        detail.append('\n');

        detail.append("=== USB ACCESSORIES (AOA) ===\n");
        if (accessories != null && accessories.length > 0) {
            for (UsbAccessory accessory : accessories) {
                detail.append("  Manufacturer: ").append(safe(accessory.getManufacturer()))
                        .append("  Model: ").append(safe(accessory.getModel())).append('\n');
            }
        } else {
            detail.append("(none)\n");
        }
        detail.append('\n');

        detail.append("=== EVENT LOG ===\n").append(eventLog);

        detailsText.setText(detail.toString());

        // Choose the visual state (most informative match wins).
        if (tvCandidate != null) {
            showTvDetected(tvCandidate);
        } else if (accessories != null && accessories.length > 0) {
            showAccessory(accessories[0]);
        } else if (!hostDevices.isEmpty()) {
            showUsbDeviceConnected(hostDevices.values().iterator().next());
        } else if (usbConnected) {
            showPeripheralConnected();
        } else {
            showNoUsb();
        }
    }

    // ------------------------------------------------------------- UI states

    private void showTvDetected(UsbDevice device) {
        String manufacturer = safe(device.getManufacturerName());
        String product = safe(device.getProductName());
        String name;
        if (!manufacturer.isEmpty() && !product.isEmpty()) {
            name = manufacturer + " " + product;
        } else if (!product.isEmpty()) {
            name = product;
        } else if (!manufacturer.isEmpty()) {
            name = manufacturer;
        } else {
            name = device.getDeviceName();
        }
        logEvent("TV candidate detected: " + name);
        setStatus(R.color.status_success, getString(R.string.status_tv_detected), name,
                String.format(Locale.US, getString(R.string.tv_detected_description),
                        hex4(device.getVendorId()), hex4(device.getProductId())));
    }

    private void showUsbDeviceConnected(UsbDevice device) {
        String name = safe(device.getProductName());
        if (name.isEmpty()) {
            name = safe(device.getManufacturerName());
        }
        if (name.isEmpty()) {
            name = device.getDeviceName();
        }
        setStatus(R.color.status_connected, getString(R.string.status_usb_host),
                name, getString(R.string.usb_host_description));
    }

    private void showAccessory(UsbAccessory accessory) {
        String name = safe(accessory.getModel());
        if (name.isEmpty()) {
            name = safe(accessory.getManufacturer());
        }
        if (name.isEmpty()) {
            name = "USB accessory";
        }
        setStatus(R.color.status_connected, getString(R.string.status_accessory),
                name, getString(R.string.accessory_description));
    }

    private void showPeripheralConnected() {
        setStatus(R.color.status_connected, getString(R.string.status_usb_connected),
                getString(R.string.peripheral_name), getString(R.string.peripheral_description));
    }

    private void showNoUsb() {
        setStatus(R.color.status_default, getString(R.string.status_title_default),
                getString(R.string.tv_name_default), getString(R.string.tv_description_default));
    }

    private void setStatus(int colorRes, String title, String name, String description) {
        int color = ContextCompat.getColor(this, colorRes);
        statusDot.getBackground().setTint(color);
        statusTitle.setText(title);
        statusTitle.setTextColor(color);
        tvName.setText(name);
        tvDescription.setText(description);
    }

    // -------------------------------------------------------------- helpers

    private static boolean looksLikeTv(UsbDevice device) {
        String manufacturer = safe(device.getManufacturerName()).toLowerCase(Locale.US);
        String product = safe(device.getProductName()).toLowerCase(Locale.US);
        String combined = manufacturer + " " + product;
        for (String brand : TV_BRAND_KEYWORDS) {
            if (combined.contains(brand)) {
                return true;
            }
        }
        if (combined.contains("tv") || combined.contains("display")
                || combined.contains("television") || combined.contains("monitor")) {
            return true;
        }
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            int cls = device.getInterface(i).getInterfaceClass();
            if (cls == UsbConstants.USB_CLASS_AUDIO || cls == UsbConstants.USB_CLASS_VIDEO) {
                return true;
            }
        }
        return false;
    }

    private static String describeDevice(UsbDevice device) {
        StringBuilder sb = new StringBuilder();
        sb.append("  Device: ").append(device.getDeviceName()).append('\n');
        sb.append("    VID 0x").append(hex4(device.getVendorId()))
                .append("  PID 0x").append(hex4(device.getProductId()))
                .append("  class 0x").append(hex2(device.getDeviceClass())).append('\n');
        sb.append("    Manufacturer: ")
                .append(safe(device.getManufacturerName()).isEmpty() ? "(unknown)"
                        : device.getManufacturerName())
                .append('\n');
        sb.append("    Product: ")
                .append(safe(device.getProductName()).isEmpty() ? "(unknown)"
                        : device.getProductName())
                .append('\n');
        sb.append("    Interfaces:");
        int count = device.getInterfaceCount();
        if (count == 0) {
            sb.append(" none\n");
        } else {
            for (int i = 0; i < count; i++) {
                UsbInterface iface = device.getInterface(i);
                sb.append("\n      #").append(i)
                        .append(" class 0x").append(hex2(iface.getInterfaceClass()))
                        .append(" (").append(interfaceClassName(iface.getInterfaceClass()))
                        .append(')');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String interfaceClassName(int cls) {
        switch (cls) {
            case UsbConstants.USB_CLASS_AUDIO: return "audio";
            case UsbConstants.USB_CLASS_COMM: return "communications";
            case UsbConstants.USB_CLASS_HID: return "hid";
            case UsbConstants.USB_CLASS_PHYSICA: return "physical";
            case UsbConstants.USB_CLASS_STILL_IMAGE: return "image";
            case UsbConstants.USB_CLASS_PRINTER: return "printer";
            case UsbConstants.USB_CLASS_MASS_STORAGE: return "mass storage";
            case UsbConstants.USB_CLASS_HUB: return "hub";
            case UsbConstants.USB_CLASS_CDC_DATA: return "cdc data";
            case UsbConstants.USB_CLASS_CSCID: return "smart card";
            case UsbConstants.USB_CLASS_CONTENT_SEC: return "content security";
            case UsbConstants.USB_CLASS_VIDEO: return "video";
            case UsbConstants.USB_CLASS_VENDOR_SPEC: return "vendor-specific";
            default: return "0x" + hex2(cls);
        }
    }

    private static String dumpUsbStateExtras(Intent state) {
        Bundle extras = state.getExtras();
        if (extras == null) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (String key : extras.keySet()) {
            Object value = extras.get(key);
            if (value instanceof Boolean || value instanceof String || value instanceof Integer) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(key).append('=').append(value);
            }
        }
        return sb.length() == 0 ? "none" : sb.toString();
    }

    private void logEvent(String message) {
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        if (eventLog.length() > 0) {
            eventLog.append('\n');
        }
        eventLog.append(ts).append("  ").append(message);

        String[] lines = eventLog.toString().split("\n");
        if (lines.length > MAX_EVENT_LOG_LINES) {
            StringBuilder trimmed = new StringBuilder();
            for (int i = lines.length - MAX_EVENT_LOG_LINES; i < lines.length; i++) {
                if (trimmed.length() > 0) {
                    trimmed.append('\n');
                }
                trimmed.append(lines[i]);
            }
            eventLog.setLength(0);
            eventLog.append(trimmed);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String hex4(int value) {
        return String.format(Locale.US, "%04X", value & 0xFFFF);
    }

    private static String hex2(int value) {
        return String.format(Locale.US, "%02X", value & 0xFF);
    }
}
