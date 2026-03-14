package com.glass.engine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import com.blankj.molihuan.utilcode.util.ToastUtils;
import com.glass.engine.utils.FPrefs;
import com.glass.engine.utils.NetworkConnection;
import com.google.android.material.color.DynamicColors;
import com.topjohnwu.superuser.Shell;
import com.vbox.VBoxCore;
import com.vbox.app.configuration.ClientConfiguration;
import com.vbox.core.system.api.MetaActivationManager;

import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

@Obfuscate
public class BoxApplication extends MultiDexApplication {

    static {
        try {
            System.loadLibrary("client");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    public static BoxApplication gApp;

    private boolean isNetworkConnected = false;

    public static native String ApiKeyBox();

    public static BoxApplication get() {
        return gApp;
    }

    public boolean isInternetAvailable() {
        return isNetworkConnected;
    }

    public void setInternetAvailable(boolean connected) {
        isNetworkConnected = connected;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        MultiDex.install(base);
        FPrefs.with(base);

        try {
            VBoxCore.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean setHideRoot() {
                    return true;
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

                @Override
                public boolean requestInstallPackage(File file) {
                    PackageInfo info = base.getPackageManager()
                            .getPackageArchiveInfo(file.getAbsolutePath(), 0);
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    @Override
    public void onCreate() {
        super.onCreate();
        gApp = this;

        try {
            VBoxCore.get().doCreate();
        } catch (Throwable t) {
            Log.e("BoxApplication", "VBoxCore.doCreate() failed: " + t.getMessage());
        }

        Context context = getApplicationContext();
        String realProcessName = null;

        try {
            Method m = VBoxCore.class.getDeclaredMethod("getProcessName", Context.class);
            m.setAccessible(true);
            realProcessName = (String) m.invoke(null, context);
            Log.i("BBX_Process", "Real process name: " + realProcessName);
        } catch (Throwable e) {
            Log.e("BBX_Process", "Failed to get process name", e);
        }

        if ("com.miniclip.eightballpool".equals(realProcessName)) {
            new Thread(() -> {
                try {
                    System.load("hunt");
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(
                                    getApplicationContext(),
                                    "Welcome to Glass Engine !",
                                    Toast.LENGTH_SHORT
                            ).show()
                    );
                } catch (UnsatisfiedLinkError e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(
                                    getApplicationContext(),
                                    "❌ Failed to load library!",
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                }
            }).start();
        }

        MetaActivationManager.activateSdk(ApiKeyBox());
        DynamicColors.applyToActivitiesIfAvailable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        NetworkConnection.CheckInternet network =
                new NetworkConnection.CheckInternet(this);
        network.registerNetworkCallback();
    }

    public boolean checkRootAccess() {
        try {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void doExe(String shell) {
        try {
            if (checkRootAccess()) {
                String normalized = shell == null ? "" : shell.trim();
                if (normalized.startsWith("su -c ")) {
                    normalized = normalized.substring(6).trim();
                    if ((normalized.startsWith("\"") && normalized.endsWith("\"")) ||
                        (normalized.startsWith("'") && normalized.endsWith("'"))) {
                        normalized = normalized.substring(1, normalized.length() - 1);
                    }
                }
                Shell.Result result = Shell.su(normalized).exec();
                if (result.getCode() != 0) {
                    Log.w("BoxApplication", "Shell failed: " + result.getCode());
                }
            } else {
                Runtime.getRuntime().exec(shell);
            }
        } catch (Exception e) {
            Log.e("BoxApplication", "doExe error: " + e.getMessage());
        }
    }

    public void doExecute(String shell) {
        doChmod(shell, 777);
        doExe(shell);
    }

    public void doChmod(String shell, int mask) {
        doExe("chmod " + mask + " " + shell);
    }

    public void toast(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void showToastWithImage(int id, CharSequence msg) {
        ToastUtils.make()
                .setBgColor(android.R.color.white)
                .setLeftIcon(id)
                .setTextColor(android.R.color.black)
                .setNotUseSystemToast()
                .show(msg);
    }

    public static String copyCacertFromAssetsIfNeeded(Context context) {
        try {
            File file = new File(context.getFilesDir(), "cacert.pem");
            if (file.exists()) {
                return file.getAbsolutePath();
            }

            byte[] buffer = new byte[1024];
            int len;

            try (var in = context.getAssets().open("cacert.pem");
                 var out = new java.io.FileOutputStream(file)) {
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }

            return file.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }
}