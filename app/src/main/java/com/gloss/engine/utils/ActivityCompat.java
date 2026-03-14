package com.glass.engine.utils;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.multidex.BuildConfig;

import com.blankj.molihuan.utilcode.util.ToastUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.glass.engine.R;
import com.glass.engine.utils.FPrefs;

import java.util.Objects;
import java.util.List;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

public class ActivityCompat extends AppCompatActivity {

    private static ActivityCompat activityCompat;

    public static final int REQUEST_OVERLAY_PERMISSION = 5469;
    public static final int PERMISSION_REQUEST_STORAGE = 100;
    public static final int REQUEST_MANAGE_UNKNOWN_APP_SOURCES = 200;
    public static final int REQUEST_MANAGE_ALL_FILES = 300; // Новый запрос для Android 11+
    public static final int REQUEST_USAGE_ACCESS = 400; // Запрос для Usage Access
    public static final int REQUEST_BATTERY_OPTIMIZATION = 500; // Запрос для Battery Optimization

    public boolean isLogin = true;
    public FPrefs prefs;
    private BottomSheetDialog bottomSheetDialog;

    public static String name;
    public static int version;
    public static String url;

    public static ActivityCompat getActivityCompat() {
        return activityCompat;
    }

    public FPrefs getPref() {
        return FPrefs.with(this);
    }

    @SuppressLint("ObsoleteSdkInt")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityCompat = this;
        // Убираем блокировку ориентации отсюда - она должна быть в конкретных активностях
        super.onCreate(savedInstanceState);
        // Весь fullscreen и getWindow() код теперь должен быть в дочерних Activity после setContentView
        prefs = getPref();
        
        // Проверяем, что все разрешения уже предоставлены
        if (areAllPermissionsGranted()) {
            android.util.Log.d("ActivityCompat", "All permissions already granted, skipping permission requests");
            return;
        }
        
        ManageFiles();
    }

    @SuppressLint("ResourceAsColor")
    public void toastImage(int id, CharSequence msg) {
        ToastUtils _toast = ToastUtils.make();
        _toast.setBgColor(android.R.color.white);
        _toast.setLeftIcon(id);
        _toast.setTextColor(android.R.color.black);
        _toast.setNotUseSystemToast();
        _toast.show(msg);
    }

    private boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK)
                >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    @SuppressLint("ObsoleteSdkInt")
    public void takeFilePermissions() {
        // Проверяем, что разрешение еще не предоставлено
        if (isPermissionGaranted()) {
            showBottomSheetDialog(ContextCompat.getDrawable(this, R.drawable.ic_files), "Storage Access Required", "To continue, please grant the app access to your device storage. This is needed to manage files properly.", false, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", ActivityCompat.this.getPackageName(), null);
                        intent.setData(uri);
                        ActivityCompat.this.startActivity(intent);
                    } else {
                        androidx.core.app.ActivityCompat.requestPermissions(ActivityCompat.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 1);
                    }
                    ActivityCompat.this.dismissBottomSheetDialog();
                }
            }, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.this.finish();
                    // System.exit(0); // Убираем принудительное закрытие
                }
            });
        } else {
            // Если разрешение уже предоставлено, переходим к следующему
            android.util.Log.d("ActivityCompat", "Storage permission already granted, going to install permission");
            InstllUnknownApp();
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public boolean isPermissionGaranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public void InstllUnknownApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Проверяем, что разрешение еще не предоставлено
            if (!getPackageManager().canRequestPackageInstalls()) {
                showBottomSheetDialog(ContextCompat.getDrawable(this, R.drawable.ic_unknown), "Install Unknown Apps", "Please allow installation from unknown sources to continue.", false, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ActivityCompat.this.getPackageName()));
                        ActivityCompat.this.startActivityForResult(intent, REQUEST_MANAGE_UNKNOWN_APP_SOURCES);
                        ActivityCompat.this.dismissBottomSheetDialog();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.this.finish();
                        // System.exit(0); // Убираем принудительное закрытие
                    }
                });
            } else {
                // Если разрешение уже предоставлено, переходим к следующему
                android.util.Log.d("ActivityCompat", "REQUEST_INSTALL_PACKAGES already granted, going to overlay");
                OverlayPermision();
            }
        } else {
            // Для Android ниже 8.0 сразу переходим к overlay
            android.util.Log.d("ActivityCompat", "Android < 8.0, skipping REQUEST_INSTALL_PACKAGES");
            OverlayPermision();
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public void OverlayPermision() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Проверяем, что разрешение еще не предоставлено
            if (!Settings.canDrawOverlays(this)) {
                showBottomSheetDialog(ContextCompat.getDrawable(this, R.drawable.ic_overlay), "Floating Window Permission", "To enable the app's full functionality, please allow the floating window permission in your device settings.", false, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ActivityCompat.this.getPackageName()));
                        ActivityCompat.this.startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                        ActivityCompat.this.dismissBottomSheetDialog();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.this.finish();
                        // System.exit(0); // Убираем принудительное закрытие
                    }
                });
            } else {
                // Если разрешение уже предоставлено, завершаем процесс запроса разрешений
                // Здесь можно добавить логику для перехода к основному функционалу приложения
                android.util.Log.d("ActivityCompat", "All required permissions granted");
            }
        } else {
            // Для Android ниже 6.0 завершаем процесс запроса разрешений
            android.util.Log.d("ActivityCompat", "All required permissions granted (pre-Marshmallow)");
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public void ManageFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE);
            } else {
                // После получения WRITE_EXTERNAL_STORAGE проверяем MANAGE_EXTERNAL_STORAGE для Android 11+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        requestManageAllFilesPermission();
                                    } else {
                    // Проверяем Usage Access разрешение
                    checkUsageAccessPermission();
                }
            } else {
                // Проверяем Usage Access разрешение
                checkUsageAccessPermission();
            }
            }
        } else {
            // Для Android ниже 6.0 сразу переходим к OverlayPermision
            OverlayPermision();
        }
    }

    /**
     * Проверяет разрешение Usage Access
     */
    @SuppressLint("ObsoleteSdkInt")
    private void checkUsageAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            
            // Проверяем, можем ли мы получить статистику использования
            List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60 * 60 * 24, time);
            
            if (stats == null || stats.isEmpty()) {
                // Разрешение не предоставлено, запрашиваем его
                requestUsageAccessPermission();
            } else {
                // Разрешение предоставлено, переходим к следующему шагу
                checkBatteryOptimization();
            }
        } else {
            // Для Android ниже 5.0 переходим к следующему шагу
            checkBatteryOptimization();
        }
    }
    
    /**
     * Запрашивает разрешение Usage Access
     */
    private void requestUsageAccessPermission() {
        showBottomSheetDialog(
            ContextCompat.getDrawable(this, R.drawable.ic_files),
            "Usage Access Permission",
            "To monitor app usage and provide better functionality, please grant the 'Usage Access' permission. This is required for full functionality.",
            false,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        ActivityCompat.this.startActivityForResult(intent, REQUEST_USAGE_ACCESS);
                    } catch (Exception e) {
                        // Fallback для устройств, которые не поддерживают ACTION_USAGE_ACCESS_SETTINGS
                        Intent intent = new Intent("android.settings.USAGE_ACCESS_SETTINGS");
                        ActivityCompat.this.startActivityForResult(intent, REQUEST_USAGE_ACCESS);
                    }
                    ActivityCompat.this.dismissBottomSheetDialog();
                }
            },
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.this.finish();
                }
            }
        );
    }
    
    /**
     * Проверяет Battery Optimization
     */
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String packageName = getPackageName();
            
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                requestBatteryOptimizationPermission();
            } else {
                // Все разрешения предоставлены, переходим к Overlay
                OverlayPermision();
            }
        } else {
            // Для Android ниже 6.0 переходим к Overlay
            OverlayPermision();
        }
    }
    
    /**
     * Запрашивает разрешение Battery Optimization
     */
    private void requestBatteryOptimizationPermission() {
        showBottomSheetDialog(
            ContextCompat.getDrawable(this, R.drawable.ic_files),
            "Battery Optimization",
            "To ensure the app works properly in the background, please disable battery optimization for this app.",
            false,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        ActivityCompat.this.startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION);
                    } catch (Exception e) {
                        // Fallback для устройств, которые не поддерживают ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        ActivityCompat.this.startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATION);
                    }
                    ActivityCompat.this.dismissBottomSheetDialog();
                }
            },
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.this.finish();
                }
            }
        );
    }
    
    /**
     * Запрашивает разрешение "Manage all files" для Android 11+
     */
    @SuppressLint("ObsoleteSdkInt")
    private void requestManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showBottomSheetDialog(
                ContextCompat.getDrawable(this, R.drawable.ic_files),
                "Manage All Files Permission",
                "To access all files on your device, please grant the 'Manage all files' permission. This is required for full functionality.",
                false,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            Uri uri = Uri.fromParts("package", ActivityCompat.this.getPackageName(), null);
                            intent.setData(uri);
                            ActivityCompat.this.startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES);
                        } catch (Exception e) {
                            // Fallback для устройств, которые не поддерживают ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            ActivityCompat.this.startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES);
                        }
                        ActivityCompat.this.dismissBottomSheetDialog();
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.this.finish();
                    }
                }
            );
        } else {
            // Для Android ниже 11.0 переходим к следующему разрешению
            OverlayPermision();
        }
    }

    public void showBottomSheetDialog(Drawable icon, String title, String msg, boolean cancelable, android.content.DialogInterface.OnClickListener listener, android.content.DialogInterface.OnClickListener listenerCancle) {
        if (BuildConfig.VERSION_CODE == 200) {
            return;
        }

        bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        bottomSheetDialog.setCancelable(cancelable);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_layout);

        ImageView img = bottomSheetDialog.findViewById(R.id.icon);
        if (icon != null) {
            Objects.requireNonNull(img).setImageDrawable(icon);
        }

        TextView title_tv = bottomSheetDialog.findViewById(R.id.title);
        Objects.requireNonNull(title_tv).setText(title);

        TextView msg_tv = bottomSheetDialog.findViewById(R.id.msg);
        Objects.requireNonNull(msg_tv).setText(msg);

        MaterialButton download = bottomSheetDialog.findViewById(R.id.btn);
        if (listener != null) {
            Objects.requireNonNull(download).setOnClickListener(v -> listener.onClick(bottomSheetDialog, android.content.DialogInterface.BUTTON_POSITIVE));
        }

        MaterialButton cancle = bottomSheetDialog.findViewById(R.id.btn_cancle);
        if (listenerCancle != null) {
            Objects.requireNonNull(cancle).setOnClickListener(v -> listenerCancle.onClick(bottomSheetDialog, android.content.DialogInterface.BUTTON_NEGATIVE));
        } else {
            Objects.requireNonNull(cancle).setVisibility(View.GONE);
        }

        bottomSheetDialog.show();
    }

    public void dismissBottomSheetDialog() {
        try {
            if (bottomSheetDialog != null) {
                if (bottomSheetDialog.isShowing()) {
                    bottomSheetDialog.dismiss();
                }
                bottomSheetDialog = null;
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Проверяет, что все необходимые разрешения предоставлены
     */
    private boolean areAllPermissionsGranted() {
        // Проверяем разрешение на запись в хранилище
        boolean storagePermission = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M ||
                androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;

        // Проверяем разрешение на установку неизвестных приложений (Android 8.0+)
        boolean installPermission = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ||
                getPackageManager().canRequestPackageInstalls();

        // Проверяем разрешение на overlay (Android 6.0+)
        boolean overlayPermission = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M ||
                android.provider.Settings.canDrawOverlays(this);

        // Проверяем разрешение на управление файлами (Android 11+)
        boolean manageFilesPermission = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R ||
                android.os.Environment.isExternalStorageManager();

        android.util.Log.d("ActivityCompat", "Permissions check: storage=" + storagePermission + 
                ", install=" + installPermission + ", overlay=" + overlayPermission + 
                ", manageFiles=" + manageFilesPermission);

        return storagePermission && installPermission && overlayPermission && manageFilesPermission;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (android.provider.Settings.canDrawOverlays(this)) {
                InstllUnknownApp();
            }
        } else if (requestCode == REQUEST_MANAGE_UNKNOWN_APP_SOURCES) {
            // Проверяем результат запроса разрешения на установку неизвестных приложений
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (getPackageManager().canRequestPackageInstalls()) {
                    android.util.Log.d("ActivityCompat", "REQUEST_INSTALL_PACKAGES permission granted");
                    // Переходим к следующему разрешению
                    OverlayPermision();
                } else {
                    android.util.Log.d("ActivityCompat", "REQUEST_INSTALL_PACKAGES permission denied");
                    // Пользователь отказался, но продолжаем
                    OverlayPermision();
                }
            } else {
                // Для Android ниже 8.0 переходим к следующему разрешению
                OverlayPermision();
            }
        } else if (requestCode == REQUEST_MANAGE_ALL_FILES) {
            // Проверяем результат запроса MANAGE_EXTERNAL_STORAGE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    android.util.Log.d("ActivityCompat", "MANAGE_EXTERNAL_STORAGE permission granted");
                    // Переходим к следующему разрешению
                    checkUsageAccessPermission();
                } else {
                    android.util.Log.d("ActivityCompat", "MANAGE_EXTERNAL_STORAGE permission denied");
                    // Пользователь отказался, но продолжаем
                    checkUsageAccessPermission();
                }
            } else {
                // Для Android ниже 11.0 переходим к следующему разрешению
                checkUsageAccessPermission();
            }
        } else if (requestCode == REQUEST_USAGE_ACCESS) {
            // Проверяем результат запроса Usage Access
            checkUsageAccessPermission();
        } else if (requestCode == REQUEST_BATTERY_OPTIMIZATION) {
            // Проверяем результат запроса Battery Optimization
            checkBatteryOptimization();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                android.util.Log.d("ActivityCompat", "WRITE_EXTERNAL_STORAGE permission granted");
                // После получения WRITE_EXTERNAL_STORAGE проверяем MANAGE_EXTERNAL_STORAGE для Android 11+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (!Environment.isExternalStorageManager()) {
                        requestManageAllFilesPermission();
                    } else {
                        // Переходим к следующему разрешению
                        OverlayPermision();
                    }
                } else {
                    // Для Android ниже 11.0 переходим к следующему разрешению
                    OverlayPermision();
                }
            } else {
                android.util.Log.d("ActivityCompat", "WRITE_EXTERNAL_STORAGE permission denied");
                // Пользователь отказался, но продолжаем
                OverlayPermision();
            }
        }
    }
}
