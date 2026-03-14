package com.glass.engine.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.glass.engine.utils.ActivityCompat;
import com.glass.engine.utils.DeviceUtils;

public class SplashActivity extends ActivityCompat {

    /* 🔥 CRITICAL: load native lib EARLY */
    static {
        try {
            System.loadLibrary("client");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Tablet / phone orientation (same as LoginActivity)
        if (DeviceUtils.isTablet(this)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // IMPORTANT: flag used inside ActivityCompat
        isLogin = false;

        super.onCreate(savedInstanceState);

        /* 🛡️ Anti-sniffer check (MUST be early) */
        if (isHttpSnifferDetected()) {
            Toast.makeText(this,
                    getString(com.glass.engine.R.string.http_sniffer_or_proxy_detected),
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        /* 🕳️ Display cutout support (copied from LoginActivity) */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        /* 🚀 Jump to MainActivity ASAP */
        getWindow().getDecorView().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 900); // < 1 second (perfect)
    }

    /* ===== SAME METHOD AS LoginActivity ===== */
    private boolean isHttpSnifferDetected() {
        String[] sniffers = {
                "com.guoshi.httpcanary",
                "com.chauncy.vpn",
                "org.webrtc.vpn",
                "com.xproxy.network"
        };

        android.content.pm.PackageManager pm = getPackageManager();
        for (String pkg : sniffers) {
            try {
                pm.getPackageInfo(pkg, 0);
                return true;
            } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {}
        }

        String proxyHost = System.getProperty("http.proxyHost");
        return proxyHost != null && !proxyHost.isEmpty();
    }
}
