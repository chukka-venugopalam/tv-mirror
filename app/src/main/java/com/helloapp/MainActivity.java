package com.helloapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Stage 2 + Stage 3.
 *
 * Stage 2 - USB connection detection:
 *   Detects when the phone is connected to another device (e.g. an Android Smart TV)
 *   over a USB-C cable. If the phone is the USB HOST it can read the connected
 *   device's manufacturer/product name; in peripheral mode it can only report that
 *   a connection exists; with nothing connected it shows a safe default state.
 *
 * Stage 3 - ADB test tap:
 *   The phone app sends one HTTP POST to a small relay that runs on your laptop
 *   (tools/relay.py). The relay executes `adb shell input tap x y` against the TV
 *   over Wi-Fi. The phone itself never talks adb to the TV - a TV's USB port is a
 *   host port, so adb cannot travel over the phone<->TV USB cable.
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

        // Initial scan — shows a safe default state when nothing is connected.
        refreshUsbState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
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
            Intent state = registerReceiver(null, new IntentFilter(UsbManager.ACTION_USB_STATE));
            if (state != null) {
                usbConnected = state.getBooleanExtra(UsbManager.EXTRA_USB_CONNECTED, false);
                usbConfigured = state.getBooleanExtra(UsbManager.EXTRA_USB_CONFIGURED, false);
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
            case UsbConstants.USB_CLASS_PHYSICAL: return "physical";
            case UsbConstants.USB_CLASS_IMAGE: return "image";
            case UsbConstants.USB_CLASS_PRINTER: return "printer";
            case UsbConstants.USB_CLASS_MASS_STORAGE: return "mass storage";
            case UsbConstants.USB_CLASS_HUB: return "hub";
            case UsbConstants.USB_CLASS_CDC_DATA: return "cdc data";
            case UsbConstants.USB_CLASS_SMART_CARD: return "smart card";
            case UsbConstants.USB_CLASS_CONTENT_SEC: return "content security";
            case UsbConstants.USB_CLASS_VIDEO: return "video";
            case UsbConstants.USB_CLASS_PERSONAL_HEALTHCARE: return "health";
            case UsbConstants.USB_CLASS_AUDIO_VIDEO: return "audio/video";
            case UsbConstants.USB_CLASS_VENDOR_SPECIFIC: return "vendor-specific";
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
