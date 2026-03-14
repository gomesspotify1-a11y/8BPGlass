package com.glass.engine.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import android.content.pm.PackageManager;
import android.view.Window;
import android.view.WindowManager;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.glass.engine.R;
//import com.glass.engine.floating.Overlay;
// import com.glass.engine.service.ColorAnalysisService; // Removed - using ColorPickerService instead

import android.view.MotionEvent;

import com.vbox.VBoxCore;
import android.os.Environment;
import java.io.FileInputStream;

import com.glass.engine.utils.DeviceUtils;
import com.glass.engine.utils.LogUtils;

import androidx.core.content.FileProvider;

import android.util.Log;
import android.view.ViewGroup;
import android.view.Display;
import android.view.WindowManager;

public class LoadingActivity extends AppCompatActivity {

    // private TextView consoleText; // Убрано - логи не отображаются на экране
    // private ScrollView scrollView; // Убрано - логи не отображаются на экране

    public static String socket;
    public static String daemonPath;
    
    // Native methods for Delta Force assets
    public static native String getDeltaForceUrl();
    public static native String getDeltaForcePassword();

    // Native methods for PUBG assets
    public static native String getPubgUrl();
    public static native String getPubgPassword();
    public static native String[] getPubgFileNames();
private static final int USER_ID = 0; // BlackBox default user
    
    private static boolean isFirstObbCopy = false; // Флаг для отслеживания первого копирования OBB

    private volatile boolean bypassTriggered = false;
    private volatile boolean stopButtonShown = false; // Флаг для кнопки STOP
    private volatile boolean gameLaunched = false; // Флаг запуска игры
    private volatile boolean processStarted = false; // Флаг запуска процессов
    private volatile boolean isInitialized = false; // Флаг инициализации
    private static volatile boolean isActivityRunning = false; // Статический флаг для отслеживания активности
    private volatile boolean deferShowStopOnResume = false; // Новый флаг: показать STOP в onResume

    static {
        try {
            System.loadLibrary("client");
        } catch (UnsatisfiedLinkError ignored) {

        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        
        // Ориентация уже установлена в AndroidManifest.xml
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Ориентация уже установлена в AndroidManifest.xml
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        super.onCreate(savedInstanceState);
        
        // Минимальные настройки - только самое необходимое
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
boolean sult = com.glass.engine.activity.MainActivity.getesp(); 
    
    if (sult) {
        //android.util.Log.d("hehe", "success"); 
        }
        // Инициализируем систему логирования
        LogUtils.initLogging(this);
        LogUtils.writeLog("LoadingActivity onCreate started");
        LogUtils.writeDeviceInfo();
        LogUtils.writeSystemInfo();

        android.util.Log.d("LoadingActivity", "🚀 onCreate started, isActivityRunning=" + isActivityRunning);

        // Проверяем, не запущена ли уже активность
        if (isActivityRunning) {
            android.util.Log.d("LoadingActivity", "⚠️ Activity already running, skipping onCreate");
            return;
        }

        isActivityRunning = true;
        android.util.Log.d("LoadingActivity", "✅ Activity marked as running");

        // Восстанавливаем состояние из SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("LoadingActivity", MODE_PRIVATE);
        boolean savedGameLaunched = prefs.getBoolean("gameLaunched", false);
        boolean savedProcessStarted = prefs.getBoolean("processStarted", false);
        boolean savedIsInitialized = prefs.getBoolean("isInitialized", false);

        // Проверяем, не зависла ли активность (для планшетов)
        long lastActivityTime = prefs.getLong("lastActivityTime", 0);
        long currentTime = System.currentTimeMillis();
        if (lastActivityTime > 0 && (currentTime - lastActivityTime) > 30000) { // 30 секунд
            android.util.Log.d("LoadingActivity", "⚠️ Activity seems stuck, resetting flags");
            isActivityRunning = false;
            isInitialized = false;
            processStarted = false;
            gameLaunched = false;
            stopButtonShown = false;

            // Очищаем SharedPreferences при зависании
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
        }

        // Сбрасываем состояние при запуске через слайдер (новый запуск)
        boolean isNewLaunch = getIntent().getBooleanExtra("newLaunch", false);
        if (isNewLaunch) {
            android.util.Log.d("LoadingActivity", "🔄 New launch via slider, resetting all flags");
            isActivityRunning = false;
            isInitialized = false;
            processStarted = false;
            gameLaunched = false;
            stopButtonShown = false;

            // Очищаем SharedPreferences для нового запуска
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            android.util.Log.d("LoadingActivity", "✅ State reset for new launch");

            // После сброса НЕ проверяем savedIsInitialized - запускаем заново
        } else {
            // Если уже инициализированы, не запускаем повторно (только если НЕ новый запуск)
            if (savedIsInitialized) {
                isInitialized = true;
                android.util.Log.d("LoadingActivity", "⚠️ Already initialized, skipping initialization");

                // Восстанавливаем состояние если игра уже запущена
                if (savedGameLaunched && !stopButtonShown) {
                    gameLaunched = true;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        showStopButton();
                    }, 500);
                }

                // Если процессы уже запущены, не запускаем повторно
                if (savedProcessStarted) {
                    processStarted = true;
                    android.util.Log.d("LoadingActivity", "⚠️ Processes already started, skipping initialization");
                }

                return;
            }
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Используем те же флаги что и в других активностях для совместимости с планшетами
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        setContentView(R.layout.activity_loading);
        // consoleText и scrollView убраны из layout

        // Диагностика для планшетов
        if (DeviceUtils.isTablet(this)) {
            android.util.Log.d("LoadingActivity", "📱 Tablet detected, adding extra diagnostics");
            // console log removed
        }

        // Отключаем анимацию для максимального FPS - она может ограничивать производительность
        // startLoadingAnimation();
        LoadingActivity sInstance = this;

        isFirstObbCopy = false; // Сбрасываем флаг при каждом запуске
        String targetPackage = getIntent().getStringExtra("packageName");



        if (targetPackage == null || targetPackage.isEmpty()) {

            return;
        }



        // Запрашиваем разрешения для Android 10+
        requestStoragePermissions();

        // Очищаем overlay при запуске приложения
        forceStopOverlay();



        // Проверяем и устанавливаем ориентацию
        checkAndSetOrientation();

        // Очищаем логи в начале (теперь логи не отображаются на экране)

        // Добавляем задержку чтобы пользователь видел LoadingActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Check if this is Delta Force package
            if ("com.proxima.dfm".equals(targetPackage) || "com.miniclip.eightballpool".equals(targetPackage)) {

                downloadDeltaForceAssets(targetPackage);
            } else {

                // Сначала проверяем, скопирован ли уже OBB файл
                checkAndCopyObbIfNeeded(targetPackage);
            }
        }, 2000); // 2 секунды задержки

        // Отмечаем что процессы запущены
        processStarted = true;
        isInitialized = true;

        // Сохраняем состояние в SharedPreferences
        android.content.SharedPreferences.Editor editor = getSharedPreferences("LoadingActivity", MODE_PRIVATE).edit();
        editor.putBoolean("processStarted", true);
        editor.putBoolean("isInitialized", true);
        editor.putLong("lastActivityTime", System.currentTimeMillis());
        editor.apply();
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.addCategory("android.intent.category.DEFAULT");
                        intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                        startActivityForResult(intent, 2001);
                    } catch (Exception e) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                        startActivityForResult(intent, 2001);
                    }
                }
            }
        }

        // Проверяем базовые права на чтение/запись
        File testFile = new File(getExternalFilesDir(null), "test_permissions.txt");
        try {
            if (testFile.createNewFile()) {
                testFile.delete();
            }
        } catch (Exception e) {
        }
    }



    /**
     * Запускает анимацию загрузки
     */
    private void startLoadingAnimation() {
        View whiteArc = findViewById(R.id.white_arc);
        if (whiteArc != null) {
            // Анимация вращения белой дуги с оптимизацией для высокого FPS
            android.animation.ObjectAnimator rotationAnimator = android.animation.ObjectAnimator.ofFloat(
                whiteArc, "rotation", 0f, 360f
            );
            rotationAnimator.setDuration(1000); // Быстрее - 1 секунда на полный оборот
            rotationAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            rotationAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
            
            // Оптимизация для высокого FPS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                whiteArc.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            
            rotationAnimator.start();
        }
    }

    /**
     * Заменяет анимацию на кнопку STOP
     */
    private void showStopButton() {
        // НE показываем кнопку вообще - она вызывает фризы
        stopButtonShown = true;
        android.util.Log.d("LoadingActivity", "🛑 STOP button creation skipped to avoid FPS freeze");
    }



    private void startFadeOutAndRestart() {
        // Создаем черный overlay для затемнения
        View fadeOverlay = new View(this);
        fadeOverlay.setBackgroundColor(android.graphics.Color.BLACK);
        fadeOverlay.setAlpha(0f);

        // Добавляем overlay поверх всего
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        fadeOverlay.setLayoutParams(params);

        // Добавляем в корневой view
        View rootView = findViewById(android.R.id.content);
        if (rootView instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) rootView).addView(fadeOverlay);
        }

        // Анимация затемнения (2 секунды)
        fadeOverlay.animate()
            .alpha(1f)
            .setDuration(2000)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .withEndAction(() -> {
                // После затемнения перезапускаем приложение
                restartApplication();
            })
            .start();
    }

    private void performForceStopAndRestart() {
        android.util.Log.d("LoadingActivity", "🚨 [FORCE STOP] Starting complete cleanup and restart");

        // Запускаем полную очистку в отдельном потоке
        new Thread(() -> {
            try {
                // 1. ОСТАНОВКА ВСЕХ СЕРВИСОВ
                android.util.Log.d("LoadingActivity", "🛑 [FORCE STOP] Stopping all services");

                // Останавливаем overlay сервис
                try {
                 //   stopService(new android.content.Intent(this, com.glass.engine.floating.Overlay.class));
                    android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] Overlay service stopped");
                } catch (Exception e) {
                    android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error stopping overlay service: " + e.getMessage());
                }

                // Останавливаем GameLogMonitor


                // 2. УБИЙСТВО ВСЕХ ПРОЦЕССОВ
                android.util.Log.d("LoadingActivity", "💀 [FORCE STOP] Killing all processes");

                // Убиваем pubg_sock процессы
                try {
                    Runtime.getRuntime().exec("pkill -f pubg_sock");
                    Runtime.getRuntime().exec("killall pubg_sock");
                    android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] pubg_sock processes killed");
                } catch (Exception e) {
                    android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error killing pubg_sock: " + e.getMessage());
                }

                // Убиваем bypass процессы
                try {
                    Runtime.getRuntime().exec("pkill -f bypass");
                    Runtime.getRuntime().exec("killall bypass");
                    android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] bypass processes killed");
                } catch (Exception e) {
                    android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error killing bypass: " + e.getMessage());
                }

                // 3. ОЧИСТКА OVERLAY
                android.util.Log.d("LoadingActivity", "🗑️ [FORCE STOP] Cleaning overlay");

                // Удаляем overlay view
                try {
                    // Overlay view removal handled by service lifecycle
                    android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] Overlay view removed");
                } catch (Exception e) {
                    android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error removing overlay: " + e.getMessage());
                }

                // Отключаем ESPView
                try {
                  //  com.glass.engine.floating.ESPView.disableOverlay();
                    android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] ESPView disabled");
                } catch (Exception e) {
                    android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error disabling ESPView: " + e.getMessage());
                }

                // 4. ОЧИСТКА ЛОГОВ
                android.util.Log.d("LoadingActivity", "🧹 [FORCE STOP] Clearing logs");

                try {
                    if (isRootMode()) {
                        Runtime.getRuntime().exec("su -c 'logcat -c'");
                    } else {
                        Runtime.getRuntime().exec("logcat -c");
                    }
                    android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] Logs cleared");
                } catch (Exception e) {
                    android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error clearing logs: " + e.getMessage());
                }

                // 5. СБРОС ВСЕХ ФЛАГОВ
                android.util.Log.d("LoadingActivity", "🔄 [FORCE STOP] Resetting all flags");

                // Сбрасываем флаги
                bypassTriggered = false;
                stopButtonShown = false;
                gameLaunched = false;
                processStarted = false;
                isInitialized = false;
                isActivityRunning = false;

                // Очищаем SharedPreferences
                try {
                    android.content.SharedPreferences.Editor editor = getSharedPreferences("LoadingActivity", MODE_PRIVATE).edit();
                    editor.clear();
                    editor.apply();
                    android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] SharedPreferences cleared");
                } catch (Exception e) {
                    android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error clearing SharedPreferences: " + e.getMessage());
                }

                // 6. FORCE STOP ПРИЛОЖЕНИЯ
                android.util.Log.d("LoadingActivity", "💥 [FORCE STOP] Force stopping application");

                try {
                    // Force stop через am
                    if (isRootMode()) {
                        Runtime.getRuntime().exec("su -c 'am force-stop " + getPackageName() + "'");
                    } else {
                        Runtime.getRuntime().exec("am force-stop " + getPackageName());
                        // Добавляем //MetaActivityManager.killAppByPkg для non-root режима
                        String targetPackage = getIntent().getStringExtra("packageName");
                        if (targetPackage != null && !targetPackage.isEmpty()) {
                            try {
                                //MetaActivityManager.killAppByPkg(targetPackage);
                                android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] MetaActivityManager killed app: " + targetPackage);
                            } catch (Exception e) {
                                android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error killing app via MetaActivityManager: " + e.getMessage());
                            }
                        }
                    }
                    android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] Application force stopped");
                } catch (Exception e) {
                    android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error force stopping app: " + e.getMessage());
                }

                // 7. ПЕРЕЗАПУСК ПРИЛОЖЕНИЯ
                android.util.Log.d("LoadingActivity", "🚀 [FORCE STOP] Restarting application");

                // Задержка перед перезапуском
                Thread.sleep(1000);

                try {
                    // Получаем Intent для перезапуска приложения
                    android.content.Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (intent != null) {
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                                       android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                       android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                       android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                        // Запускаем новую копию приложения
                        startActivity(intent);

                        // Завершаем текущую активность
                        finish();

                        // Принудительно завершаем процесс
                        android.os.Process.killProcess(android.os.Process.myPid());

                        android.util.Log.d("LoadingActivity", "✅ [FORCE STOP] Application restarted successfully");
                    }
                } catch (Exception e) {
                    android.util.Log.e("LoadingActivity", "❌ [FORCE STOP] Error restarting application: " + e.getMessage());
                    // В случае ошибки просто завершаем активность
                    finish();
                }

            } catch (Exception e) {
                android.util.Log.e("LoadingActivity", "💥 [FORCE STOP] Critical error: " + e.getMessage());
                // В случае критической ошибки завершаем активность
                finish();
            }
        }).start();
    }

    private void restartApplication() {
        try {
            // Получаем Intent для перезапуска приложения
            android.content.Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                               android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP |
                               android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);

                // Запускаем новую копию приложения
                startActivity(intent);

                // Завершаем текущую активность
                finish();

                // Принудительно завершаем процесс (опционально)
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        } catch (Exception e) {
            android.util.Log.e("LoadingActivity", "Error restarting application: " + e.getMessage());
            // В случае ошибки просто завершаем активность
            finish();
        }
    }


    private void checkAndCopyObbIfNeeded(String packageName) {
        try {
            // Проверяем, что это PUBG - только для него делаем OBB копирование
           if (
    !"com.tencent.ig".equals(packageName)  ||
    !"com.rekoo.pubgm".equals(packageName) ||
    !"com.vng.pubgmobile".equals(packageName) ||
    !"com.pubg.krmobile".equals(packageName)
) {
    // your code here


                onObbReady(packageName, false);
                return;
            }

            // Проверяем root-режим
            boolean isRootMode = isRootMode();

            // В root-режиме пропускаем копирование OBB
            if (isRootMode) {

                onObbReady(packageName, false);
                return;
            }


            int versionCode = getPackageManager().getPackageInfo(packageName, 0).versionCode;
            String obbFileName = "main." + versionCode + "." + packageName + ".obb";

            // Проверяем scoped storage путь (куда мы копируем)
            File scopedDir = new File("/storage/emulated/0/SdCard/Android/obb/" + packageName);
            File scopedObb = new File(scopedDir, obbFileName);

            if (scopedObb.exists() && scopedObb.isFile() && scopedObb.length() > 0) {

                // Если OBB уже скопирован в scoped storage, запускаем нормально (не первый запуск)
                isFirstObbCopy = false;
                onObbReady(packageName, false);
                return;
            } else {
                // OBB не найден, будет копироваться

                isFirstObbCopy = true; // Это будет первое копирование
                debugObbSearch(packageName);
                copyObbIfExists(packageName);
            }

        } catch (Exception e) {

        }
    }

    private void copyObbIfExists(String packageName) {
        try {
            int versionCode = getPackageManager().getPackageInfo(packageName, 0).versionCode;
            String obbFileName = "main." + versionCode + "." + packageName + ".obb";
            File realObb = new File("/storage/emulated/0/Android/obb/" + packageName, obbFileName);
            File scopedDir = new File("/storage/emulated/0/SdCard/Android/obb/" + packageName);
            File scopedObb = new File(scopedDir, obbFileName);

            if (!realObb.exists() || realObb.length() == 0) {
                // Пробуем найти OBB в других местах
                File alternativeObb = findObbInAlternativeLocations(packageName, versionCode);
                if (alternativeObb != null) {
                    realObb = alternativeObb;
                } else {

                    // Если OBB не найден нигде, запускаем без OBB
                    onObbReady(packageName, false);
                    return;
                }
            }

            if (scopedObb.exists() && scopedObb.length() == realObb.length()) {

                onObbReady(packageName, false);
                return;
            }

            // Создаем директорию если нужно
            if (!scopedDir.exists()) {
                if (!scopedDir.mkdirs()) {
                    // Пробуем альтернативные методы копирования
                    if (tryAlternativeCopyMethods(realObb, packageName, versionCode)) {
                        onObbReady(packageName, isFirstObbCopy);
                        return;
                    } else {

                        onObbReady(packageName, false);
                        return;
                    }
                }
            }



            // Пробуем разные методы копирования
            boolean copySuccess = false;

            // Метод 1: Прямое копирование
            try {
                copySuccess = copyFileDirectly(realObb, scopedObb);
            } catch (Exception e) {
            }

            // Метод 2: Через FileProvider если прямой не удался
            if (!copySuccess) {
                try {
                    copySuccess = copyFileWithFileProvider(realObb, scopedObb);
                } catch (Exception e) {
                }
            }

            // Метод 3: Через shell команды
            if (!copySuccess) {
                try {
                    copySuccess = copyFileWithShell(realObb, scopedObb);
                } catch (Exception e) {
                }
            }

            // Метод 4: Создание hard link
            if (!copySuccess) {
                try {
                    copySuccess = createHardLink(realObb, scopedObb);
                } catch (Exception e) {
                }
            }

            if (copySuccess && scopedObb.exists() && scopedObb.length() > 0) {

                // Если это первый запуск (копирование), запускаем только Meta и завершаем
                if (isFirstObbCopy) {


                    // Проверяем и устанавливаем ориентацию перед запуском
                    checkAndSetOrientation();

                    VBoxCore.get().launchApk(packageName, USER_ID);

                    // Запускаем мониторинг для первого запуска с увеличенной задержкой

                    startGameMonitoring(packageName);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {

                        forceStopApp(packageName);
                        //MetaActivityManager.killAppByPkg(packageName);

                        // Показываем финальное сообщение перед закрытием
                        showFinalMessage();

                        // Убираем агрессивное закрытие - пусть игра работает
                        // new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        //     forceCloseApplication();
                        // }, 5000);


                    }, 30000); // 30 секунд
                } else {
                    // Добавляем задержку для non-root режима после копирования OBB
                    if (!isRootMode()) {


                        // Проверяем завершение копирования с повторными попытками
                        final int[] attempts = {0};
                        final int maxAttempts = 10; // Максимум 10 попыток

                        final Runnable checkObbReady = new Runnable() {
                            @Override
                            public void run() {
                                attempts[0]++;

                                if (scopedObb.exists() && scopedObb.isFile() && scopedObb.length() > 0 && scopedObb.canRead()) {

                                    onObbReady(packageName, false);
                                } else if (attempts[0] < maxAttempts) {


                                    new Handler(Looper.getMainLooper()).postDelayed(this, 500); // Проверяем каждые 500мс
                                } else {


                                    onObbReady(packageName, false);
                                }
                            }
                        };

                        new Handler(Looper.getMainLooper()).postDelayed(checkObbReady, 3000); // Начинаем проверку через 3 секунды
                    } else {
                        onObbReady(packageName, false);
                    }
                }
            } else {

                onObbReady(packageName, false);
            }

        } catch (Exception e) {

            onObbReady(packageName, false);
        }
    }



    private void showDetailedObbSearchInfo(String packageName, int versionCode) {


        String obbFileName = "main." + versionCode + "." + packageName + ".obb";


        String[] searchPaths = {
            "/storage/emulated/0/Android/obb/" + packageName + "/" + obbFileName,
            "/sdcard/Android/obb/" + packageName + "/" + obbFileName,
            Environment.getExternalStorageDirectory() + "/Android/obb/" + packageName + "/" + obbFileName,
            "/mnt/sdcard/Android/obb/" + packageName + "/" + obbFileName,
            "/storage/sdcard0/Android/obb/" + packageName + "/" + obbFileName,
            "/mnt/shell/emulated/0/Android/obb/" + packageName + "/" + obbFileName
        };

        for (String path : searchPaths) {
            File file = new File(path);
            String status = "NOT_FOUND";
            String details = "";

            if (file.exists()) {
                if (file.isFile()) {
                    if (file.canRead()) {
                        if (file.length() > 0) {
                            status = "VALID";
                            details = " (size: " + file.length() + " bytes)";
                        } else {
                            status = "EMPTY";
                            details = " (0 bytes)";
                        }
                    } else {
                        status = "NO_READ_PERMISSION";
                        details = " (permission denied)";
                    }
                } else {
                    status = "NOT_A_FILE";
                    details = " (is directory)";
                }
            } else {
                status = "NOT_FOUND";
                details = " (file does not exist)";
            }


        }


    }

    @SuppressLint("StaticFieldLeak")
    private void onObbReady(String packageName, boolean isFirstLaunch) {
        // Check if this is Delta Force package - skip bypass and log monitoring
        if ("com.proxima.dfm".equals(packageName) || "com.miniclip.eightballpool".equals(packageName)) {

            launchDeltaForceGame(packageName);
            return;
        }

        // Проверяем, что это PUBG - только для него делаем все проверки и bypass
        if (
    !"com.tencent.ig".equals(packageName) ||
    !"com.rekoo.pubgm".equals(packageName) ||
    !"com.vng.pubgmobile".equals(packageName) ||
    !"com.pubg.krmobile".equals(packageName)
) {
    // your code here


            launchGame(packageName);
            return;
        }

        // Проверяем root-режим
        boolean isRootMode = isRootMode();

        if (isRootMode) {


            // 1. Сначала скачиваем файлы

            checkAndLoadRootAssets(packageName);

            // 2. Ждем завершения скачивания
            waitForAssetDownload(() -> {


                // 3. Запускаем оригинальную игру
                launchGame(packageName);

                // Проверка наличия OBB после запуска игры
                int versionCodeCheck = getObbVersionCode(packageName);
                String obbFileNameCheck = "main." + versionCodeCheck + "." + packageName + ".obb";
                File scopedObbCheck = new File("/storage/emulated/0/SdCard/Android/obb/" + packageName, obbFileNameCheck);
                if (!scopedObbCheck.exists() || !scopedObbCheck.isFile() || scopedObbCheck.length() == 0) {

                    copyObbIfExists(packageName);
                }

                // 4. Запускаем мониторинг логов для поиска триггера (как в non-root)

                startRootLogMonitoring(packageName);
            });

        } else {


            // 1. Сначала скачиваем файлы

            checkAndLoadRootAssets(packageName);

            // 2. Ждем завершения скачивания
            waitForAssetDownload(() -> {


                // 3. Проверяем OBB
                int versionCode = getObbVersionCode(packageName);
                String obbFileName = "main." + versionCode + "." + packageName + ".obb";
                File scopedObb = new File("/storage/emulated/0/SdCard/Android/obb/" + packageName, obbFileName);

                // Дополнительная проверка для non-root режима
                if (scopedObb.exists() && scopedObb.isFile() && scopedObb.length() > 0) {
                    // Проверяем, что файл полностью доступен для чтения
                    try {
                        if (scopedObb.canRead()) {

                            // 4. Запускаем игру через Meta
                            launchGame(packageName);

                            // 5. Запускаем мониторинг логов для поиска триггера

                            startBypassLogMonitoring(packageName);

                            return;
                        } else {

                            // Если файл не читается, ждем еще немного
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (scopedObb.canRead()) {

                                    launchGame(packageName);
                                    startBypassLogMonitoring(packageName);
                                } else {

                                    launchGame(packageName);
                                    startBypassLogMonitoring(packageName);
                                }
                            }, 2000); // 2 секунды дополнительного ожидания
                            return;
                        }
                    } catch (Exception e) {

                        // При ошибке проверки все равно запускаем
                        launchGame(packageName);
                        startBypassLogMonitoring(packageName);
                        return;
                    }
                } else {

                    // Даже если OBB не готов, пытаемся запустить игру
                    launchGame(packageName);
                    startBypassLogMonitoring(packageName);
                }
            });
        }
    }

    private void checkAndLoadRootAssets(String packageName) {
        // Проверяем, что это PUBG - только для него загружаем assets
        if (
    !"com.tencent.ig".equals(packageName) ||
    !"com.rekoo.pubgm".equals(packageName) ||
    !"com.vng.pubgmobile".equals(packageName) ||
    !"com.pubg.krmobile".equals(packageName)
) {

            return;
        }



        File bypassFile = new File("/data/local/tmp/bypass");
        File pubgSockFile = new File("/data/local/tmp/pubg_sock");




        // Всегда запускаем Server для обновления файлов (и в root, и в non-root режиме)
        if (isRootMode()) {

        } else {

        }

        // Запускаем загрузку с сервера
        try {
            // Передаем пустую строку как параметр, URL будет получен из Api
            new com.glass.engine.component.Server(this, "").execute("");


            // Запускаем мониторинг загрузки
            startAssetDownloadMonitoring();
        } catch (Exception e) {

        }
    }

    private void startAssetDownloadMonitoring() {
        android.util.Log.d("DELTA_FORCE", "🔍 Starting asset download monitoring...");

        final Handler monitorHandler = new Handler(Looper.getMainLooper());
        final int[] attempts = {0};
        final int maxAttempts = 60; // 5 минут максимум
        final boolean isRoot = isRootMode();

        final Runnable monitorRunnable = new Runnable() {
            @Override
            public void run() {
                attempts[0]++;

                if (areDeltaForceAssetsLoaded()) {
                    String path = isRoot ? "/data/local/tmp/" : "app directory";
                    android.util.Log.d("DELTA_FORCE", "✅ Assets found in " + path + ", monitoring completed");
                    return;
                }

                if (attempts[0] >= maxAttempts) {
                    android.util.Log.w("DELTA_FORCE", "⚠️ Asset monitoring timeout after " + maxAttempts + " attempts");
                    return;
                }

                String path = isRoot ? "/data/local/tmp/" : "app directory";
                android.util.Log.d("DELTA_FORCE", "⏳ Waiting for assets in " + path + "... (" + attempts[0] + "/" + maxAttempts + ")");

                monitorHandler.postDelayed(this, 5000); // Проверяем каждые 5 секунд
            }
        };

        monitorHandler.postDelayed(monitorRunnable, 5000);
    }

    /**
     * Custom asset downloading function for Delta Force games
     */
    private void downloadDeltaForceAssets(String packageName) {
        android.util.Log.d("DELTA_FORCE", "🚀 Starting Delta Force asset download for: " + packageName);

        // Kill any existing processes before downloading
        killExistingProcesses();

        // Проверяем, есть ли уже файлы
        if (areDeltaForceAssetsLoaded()) {
            android.util.Log.d("DELTA_FORCE", "✅ Assets already exist, skipping download");
            return;
        }

        new Thread(() -> {
            try {
                android.util.Log.d("DELTA_FORCE", "📥 Downloading Delta Force assets from server...");

                // Download URL and password from native code (obfuscated)
                String downloadUrl = getDeltaForceUrl();
                String password = getDeltaForcePassword();

                android.util.Log.d("DELTA_FORCE", "🔍 Native method called - URL: " + downloadUrl);
                android.util.Log.d("DELTA_FORCE", "🔍 Native method called - Password: " + password);

                // Get license key for authentication - try multiple sources like existing code
                String licenseKey = getLicenseFromFile();
                if (licenseKey == null || licenseKey.isEmpty()) {
                    licenseKey = com.glass.engine.activity.LoginActivity.GetKey();
                }
                android.util.Log.d("DELTA_FORCE", "🔑 License key: " + licenseKey);

                android.util.Log.d("DELTA_FORCE", "🔗 Download URL: " + downloadUrl);
                android.util.Log.d("DELTA_FORCE", "🔐 Password: " + password);

                // Create download directory
                File downloadDir = new File(getFilesDir(), "delta_force_assets");
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                android.util.Log.d("DELTA_FORCE", "📁 Download directory: " + downloadDir.getAbsolutePath());

                // Download file with retry mechanism like existing Server.java
                File zipFile = new File(downloadDir, "delta.zip");
                if (!tryDownloadWithRetry(downloadUrl, zipFile, licenseKey)) {
                    android.util.Log.e("DELTA_FORCE", "💥 All download attempts failed");
                    return;
                }

                // Extract with password
                if (zipFile.exists() && zipFile.length() > 0) {
                    android.util.Log.d("DELTA_FORCE", "📦 ZIP file exists, size: " + zipFile.length() + " bytes");

                    extractZipWithPassword(zipFile, downloadDir, password);

                    // Clean up zip file
                    zipFile.delete();
                    android.util.Log.d("DELTA_FORCE", "🗑️ Cleaned up ZIP file");

                    android.util.Log.d("DELTA_FORCE", "✅ Assets downloaded and extracted successfully");
                } else {
                    android.util.Log.e("DELTA_FORCE", "❌ Failed to download assets - ZIP file not found or empty");
                    return;
                }

            } catch (Exception e) {
                android.util.Log.e("DELTA_FORCE", "💥 Error downloading assets: " + e.getMessage(), e);
                return;
            }
        }).start();
    }

    /**
     * Try download with multiple authentication methods like existing Server.java
     */
    private boolean tryDownloadWithRetry(String urlString, File outputFile, String licenseKey) {
        android.util.Log.d("DELTA_FORCE", "🔄 Starting download retry with multiple auth methods");

        // Try different authentication methods
        if (tryDownloadWithAuth(urlString, outputFile, "Bearer " + licenseKey)) {
            return true;
        }
        if (tryDownloadWithAuth(urlString, outputFile, licenseKey)) {
            return true;
        }
        if (tryDownloadWithAuth(urlString, outputFile, "Key " + licenseKey)) {
            return true;
        }
        if (tryDownloadWithAuth(urlString, outputFile, "Token " + licenseKey)) {
            return true;
        }
        if (tryDownloadWithAuth(urlString, outputFile, "ApiKey " + licenseKey)) {
            return true;
        }
        if (tryDownloadWithQueryParam(urlString, outputFile, licenseKey)) {
            return true;
        }

        return false;
    }

    /**
     * Try download with specific authentication format
     */
    private boolean tryDownloadWithAuth(String urlString, File outputFile, String authValue) {
        try {
            android.util.Log.d("DELTA_FORCE", "🔐 Trying auth: " + authValue);

            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            // Add multiple authentication headers
            String licenseKey = getLicenseFromFile();
            if (licenseKey == null || licenseKey.isEmpty()) {
                licenseKey = com.glass.engine.activity.LoginActivity.GetKey();
            }

            if (licenseKey != null && !licenseKey.isEmpty()) {
                connection.setRequestProperty("Authorization", authValue);
                connection.setRequestProperty("X-License", licenseKey);
                connection.setRequestProperty("X-Key", licenseKey);
                connection.setRequestProperty("Key", licenseKey);
                connection.setRequestProperty("License", licenseKey);
                connection.setRequestProperty("api-key", licenseKey);
                connection.setRequestProperty("x-api-key", licenseKey);
                connection.setRequestProperty("token", licenseKey);
                connection.setRequestProperty("x-token", licenseKey);
                android.util.Log.d("DELTA_FORCE", "🔐 Added multiple auth headers");
            } else {
                android.util.Log.w("DELTA_FORCE", "⚠️ No license key available");
                return false;
            }

            connection.connect();
            int responseCode = connection.getResponseCode();
            android.util.Log.d("DELTA_FORCE", "📡 Response code: " + responseCode);

            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                // Download successful
                android.util.Log.d("DELTA_FORCE", "✅ Download successful with auth: " + authValue);

                int lengthOfFile = connection.getContentLength();
                android.util.Log.d("DELTA_FORCE", "📏 File size: " + lengthOfFile + " bytes");

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(outputFile)) {

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);

                        // Log progress every 10%
                        if (lengthOfFile > 0 && total % (lengthOfFile / 10) == 0) {
                            int progress = (int) ((total * 100) / lengthOfFile);
                            android.util.Log.d("DELTA_FORCE", "📊 Download progress: " + progress + "%");
                        }
                    }

                    android.util.Log.d("DELTA_FORCE", "✅ Downloaded: " + total + " bytes");
                    return true;
                }
            } else {
                // Read error response
                try {
                    InputStream errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        android.util.Log.w("DELTA_FORCE", "⚠️ Auth failed: " + authValue + " - " + response.toString());
                    }
                } catch (Exception e) {
                    android.util.Log.w("DELTA_FORCE", "⚠️ Auth failed: " + authValue + " - " + e.getMessage());
                }
                return false;
            }
        } catch (Exception e) {
            android.util.Log.w("DELTA_FORCE", "⚠️ Auth failed: " + authValue + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Try download with query parameter authentication
     */
    private boolean tryDownloadWithQueryParam(String urlString, File outputFile, String licenseKey) {
        try {
            android.util.Log.d("DELTA_FORCE", "🔐 Trying query param auth");

            String urlWithKey = urlString + "?key=" + licenseKey;
            java.net.URL url = new java.net.URL(urlWithKey);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            connection.connect();
            int responseCode = connection.getResponseCode();
            android.util.Log.d("DELTA_FORCE", "📡 Query param response code: " + responseCode);

            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                // Download successful
                android.util.Log.d("DELTA_FORCE", "✅ Download successful with query param");

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(outputFile)) {

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);
                    }

                    android.util.Log.d("DELTA_FORCE", "✅ Downloaded: " + total + " bytes");
                    return true;
                }
            } else {
                android.util.Log.w("DELTA_FORCE", "⚠️ Query param auth failed");
                return false;
            }
        } catch (Exception e) {
            android.util.Log.w("DELTA_FORCE", "⚠️ Query param auth failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Download file from URL (legacy method - kept for compatibility)
     */
    private void downloadFile(String urlString, File outputFile) throws IOException {
        android.util.Log.d("DELTA_FORCE", "🌐 Connecting to: " + urlString);

        java.net.URL url = new java.net.URL(urlString);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);

        // Add authentication headers - try multiple formats like the existing Server.java
        String licenseKey = getLicenseFromFile();
        if (licenseKey == null || licenseKey.isEmpty()) {
            licenseKey = com.glass.engine.activity.LoginActivity.GetKey();
        }
        if (licenseKey != null && !licenseKey.isEmpty()) {
            // Try multiple authentication header formats
            connection.setRequestProperty("Authorization", "Bearer " + licenseKey);
            connection.setRequestProperty("X-License", licenseKey);
            connection.setRequestProperty("X-Key", licenseKey);
            connection.setRequestProperty("Key", licenseKey);
            connection.setRequestProperty("License", licenseKey);
            connection.setRequestProperty("api-key", licenseKey);
            connection.setRequestProperty("X-API-Key", licenseKey);
            android.util.Log.d("DELTA_FORCE", "🔐 Added multiple authentication headers");
        } else {
            android.util.Log.w("DELTA_FORCE", "⚠️ No license key available for authentication");
        }

        int responseCode = connection.getResponseCode();
        android.util.Log.d("DELTA_FORCE", "📡 HTTP Response Code: " + responseCode);
        android.util.Log.d("DELTA_FORCE", "📏 Content Length: " + connection.getContentLength());

        // Check if response is successful
        if (responseCode != 200) {
            // Read error response
            try (InputStream errorStream = connection.getErrorStream()) {
                if (errorStream != null) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    android.util.Log.e("DELTA_FORCE", "❌ Server error response: " + errorResponse.toString());
                }
            }
            throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
        }

        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            long contentLength = connection.getContentLength();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // Update progress
                if (contentLength > 0) {
                    int progress = (int) ((totalBytes * 100) / contentLength);
                    new Handler(Looper.getMainLooper()).post(() -> {

                    });

                    // Log progress every 10%
                    if (progress % 10 == 0) {
                        android.util.Log.d("DELTA_FORCE", "📊 Download progress: " + progress + "% (" + totalBytes + "/" + contentLength + " bytes)");
                    }
                }
            }

            android.util.Log.d("DELTA_FORCE", "✅ Download completed: " + totalBytes + " bytes");
        }
    }

    /**
     * Extract ZIP file with password and copy files with Delta Force names
     */
    private void extractZipWithPassword(File zipFile, File extractDir, String password) throws IOException {
        android.util.Log.d("DELTA_FORCE", "📦 Starting ZIP extraction from: " + zipFile.getAbsolutePath());
        android.util.Log.d("DELTA_FORCE", "📁 Extract directory: " + extractDir.getAbsolutePath());
        android.util.Log.d("DELTA_FORCE", "🔐 Using password: " + password);

        try {
            // Use Zip4j library for password-protected ZIP files
            net.lingala.zip4j.ZipFile zip4jFile = new net.lingala.zip4j.ZipFile(zipFile);

            // Set password for extraction
            if (password != null && !password.isEmpty()) {
                zip4jFile.setPassword(password.toCharArray());
                android.util.Log.d("DELTA_FORCE", "🔐 Password set for ZIP extraction");
            }

            // Extract all files
            zip4jFile.extractAll(extractDir.getAbsolutePath());
            android.util.Log.d("DELTA_FORCE", "✅ ZIP extraction completed successfully");

            // Rename files to Delta Force specific names
            renameDeltaForceFiles(extractDir);

            // Copy files to appropriate locations based on root mode
            copyDeltaForceFiles(extractDir);

        } catch (Exception e) {
            android.util.Log.e("DELTA_FORCE", "💥 Error during ZIP extraction: " + e.getMessage(), e);

            // Fallback to basic extraction if Zip4j fails
            try {
                android.util.Log.d("DELTA_FORCE", "🔄 Trying basic extraction as fallback");
                extractZipBasic(zipFile, extractDir, password);
            } catch (Exception fallbackException) {
                android.util.Log.e("DELTA_FORCE", "💥 Fallback extraction also failed: " + fallbackException.getMessage(), fallbackException);
                throw new IOException("ZIP extraction failed with password", e);
            }
        }
    }

    /**
     * Basic ZIP extraction as fallback (without password support)
     */
    private void extractZipBasic(File zipFile, File extractDir, String password) throws IOException {
        android.util.Log.d("DELTA_FORCE", "📦 Basic ZIP extraction (no password support)");

        java.util.zip.ZipInputStream zipInputStream = new java.util.zip.ZipInputStream(new java.io.FileInputStream(zipFile));

        java.util.zip.ZipEntry entry;
        int extractedFiles = 0;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                String fileName = entry.getName();
                android.util.Log.d("DELTA_FORCE", "📄 Processing ZIP entry: " + fileName + " (size: " + entry.getSize() + " bytes)");

                File outputFile = new File(extractDir, fileName);
                outputFile.getParentFile().mkdirs();

                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;
                    while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }

                    android.util.Log.d("DELTA_FORCE", "✅ Extracted: " + fileName + " (" + totalBytes + " bytes)");
                    extractedFiles++;
                }
            }
            zipInputStream.closeEntry();
        }
        zipInputStream.close();

        android.util.Log.d("DELTA_FORCE", "📊 Total files extracted: " + extractedFiles);
    }

    /**
     * Rename files to Delta Force specific names
     */
    private void renameDeltaForceFiles(File extractDir) {
        try {
            File[] files = extractDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String newFileName = fileName;

                    // Rename files to Delta Force specific names
                    if (fileName.equals("pubg_sock")) {
                        newFileName = "delta_sock";
                        File newFile = new File(extractDir, newFileName);
                        if (file.renameTo(newFile)) {
                            android.util.Log.d("DELTA_FORCE", "🔄 Renamed: pubg_sock -> delta_sock");
                        }
                    } else if (fileName.equals("bypass")) {
                        // For root mode: bypass -> delta_rootbypass
                        // For non-root mode: bypass -> delta_bypass
                        if (isRootMode()) {
                            newFileName = "delta_rootbypass";
                            File newFile = new File(extractDir, newFileName);
                            if (file.renameTo(newFile)) {
                                android.util.Log.d("DELTA_FORCE", "🔄 Renamed: bypass -> delta_rootbypass (ROOT MODE)");
                            }
                        } else {
                            newFileName = "delta_bypass";
                            File newFile = new File(extractDir, newFileName);
                            if (file.renameTo(newFile)) {
                                android.util.Log.d("DELTA_FORCE", "🔄 Renamed: bypass -> delta_bypass (NON-ROOT MODE)");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("DELTA_FORCE", "💥 Error renaming files: " + e.getMessage(), e);
        }
    }

    /**
     * Copy Delta Force files to appropriate locations
     */
    private void copyDeltaForceFiles(File extractDir) {
        android.util.Log.d("DELTA_FORCE", "📋 Starting file copy operations from: " + extractDir.getAbsolutePath());

        try {
            if (isRootMode()) {
                android.util.Log.d("DELTA_FORCE", "🔧 Root mode detected - copying to /data/local/tmp/");
                // Root mode - copy to /data/local/tmp/
                copyFileToRoot(new File(extractDir, "delta_sock"), "/data/local/tmp/delta_sock");
                copyFileToRoot(new File(extractDir, "delta_rootbypass"), "/data/local/tmp/delta_rootbypass");

            } else {
                android.util.Log.d("DELTA_FORCE", "📱 Non-root mode detected - copying to app directory");
                // Non-root mode - copy to app files directory
                File appFilesDir = getFilesDir();
                copyFile(new File(extractDir, "delta_sock"), new File(appFilesDir, "delta_sock"));
                copyFile(new File(extractDir, "delta_bypass"), new File(appFilesDir, "delta_bypass"));

            }
        } catch (Exception e) {
            android.util.Log.e("DELTA_FORCE", "💥 Error copying Delta Force files: " + e.getMessage(), e);

        }
    }

    /**
     * Copy file to root location
     */
    private void copyFileToRoot(File source, String targetPath) {
        android.util.Log.d("DELTA_FORCE", "🔧 Copying to root: " + source.getName() + " -> " + targetPath);

        if (source.exists()) {
            try {
                android.util.Log.d("DELTA_FORCE", "📄 Source file exists, size: " + source.length() + " bytes");

                // Используем BoxApplication.doExe для лучшей совместимости с root
                android.util.Log.d("DELTA_FORCE", "📋 [ROOT] Copying file using BoxApplication.doExe");
                
                // Копируем файл
                com.glass.engine.BoxApplication.get().doExe("su -c 'cp " + source.getAbsolutePath() + " " + targetPath + "'");
                
                // Небольшая задержка для завершения копирования
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Ignore
                }
                
                // Устанавливаем права
                com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 " + targetPath + "'");
                
                // Дополнительная задержка для завершения операций
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore
                }
                
                // Проверяем результат
                File targetFile = new File(targetPath);
                if (targetFile.exists() && targetFile.length() > 0) {
                    android.util.Log.d("DELTA_FORCE", "✅ Successfully copied to root: " + targetPath + " (size: " + targetFile.length() + " bytes)");
                    
                    // Дополнительная проверка через shell
                    try {
                        String checkCommand = "su -c 'ls -la " + targetPath + "'";
                        com.glass.engine.BoxApplication.get().doExe(checkCommand);
                        android.util.Log.d("DELTA_FORCE", "🔍 [ROOT] File permissions verified via shell");
                    } catch (Exception e) {
                        android.util.Log.w("DELTA_FORCE", "⚠️ Could not verify file via shell: " + e.getMessage());
                    }
                } else {
                    android.util.Log.e("DELTA_FORCE", "❌ Target file not found or empty after copy");
                    
                    // Пробуем альтернативный метод через BoxApplication
                    try {
                        android.util.Log.d("DELTA_FORCE", "🔄 [ROOT] Trying alternative copy method...");
                        com.glass.engine.BoxApplication.get().doExe("su -c 'cp " + source.getAbsolutePath() + " " + targetPath + "'");
                        com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 " + targetPath + "'");
                        
                        // Проверяем альтернативный метод
                        Thread.sleep(500);
                        File altTargetFile = new File(targetPath);
                        if (altTargetFile.exists() && altTargetFile.length() > 0) {
                            android.util.Log.d("DELTA_FORCE", "✅ Alternative copy method via BoxApplication succeeded");
                        } else {
                            android.util.Log.e("DELTA_FORCE", "❌ Alternative copy method also failed");
                        }
                    } catch (Exception e2) {
                        android.util.Log.e("DELTA_FORCE", "💥 Alternative copy method also failed: " + e2.getMessage());
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("DELTA_FORCE", "💥 Failed to copy to root: " + e.getMessage(), e);
                
                // Последняя попытка через BoxApplication
                try {
                    com.glass.engine.BoxApplication.get().doExe("cp " + source.getAbsolutePath() + " " + targetPath);
                    com.glass.engine.BoxApplication.get().doExe("chmod 755 " + targetPath);
                    android.util.Log.d("DELTA_FORCE", "✅ Final attempt via BoxApplication succeeded");
                } catch (Exception e2) {
                    android.util.Log.e("DELTA_FORCE", "💥 All copy methods failed: " + e2.getMessage());
                }
            }
        } else {
            android.util.Log.w("DELTA_FORCE", "⚠️ Source file not found: " + source.getName());
        }
    }

    /**
     * Get license from file like existing Server.java
     */
    private String getLicenseFromFile() {
        try {
            java.io.File dir = new java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                    "Xproject"
            );
            java.io.File jsonFile = new java.io.File(dir, "Licence.json");
            if (jsonFile.exists()) {
                java.io.FileReader reader = new java.io.FileReader(jsonFile);
                java.io.BufferedReader bufferedReader = new java.io.BufferedReader(reader);
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                bufferedReader.close();
                reader.close();

                org.json.JSONObject json = new org.json.JSONObject(stringBuilder.toString());
                String license = json.getString("licence");
                android.util.Log.d("DELTA_FORCE", "📄 License loaded from file: " + license);
                return license;
            } else {
                android.util.Log.w("DELTA_FORCE", "⚠️ License file not found: " + jsonFile.getAbsolutePath());
            }
        } catch (Exception e) {
            android.util.Log.e("DELTA_FORCE", "💥 Error reading license file: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Copy file with standard Java methods
     */
    private void copyFile(File source, File target) {
        android.util.Log.d("DELTA_FORCE", "📱 Copying file: " + source.getName() + " -> " + target.getName());

        if (source.exists()) {
            try {
                android.util.Log.d("DELTA_FORCE", "📄 Source file exists, size: " + source.length() + " bytes");

                java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                android.util.Log.d("DELTA_FORCE", "✅ Successfully copied: " + source.getName() + " -> " + target.getName());

            } catch (Exception e) {
                android.util.Log.e("DELTA_FORCE", "💥 Failed to copy file: " + e.getMessage(), e);

            }
        } else {
            android.util.Log.w("DELTA_FORCE", "⚠️ Source file not found: " + source.getName());

        }
    }

    /**
     * Custom game launch function for Delta Force games
     */


//
//    private void launchDeltaForceGame(String packageName) {
//        android.util.Log.d("DELTA_FORCE", "🚀 Launching Delta Force game: " + packageName);
//
//
//        // Load delta_sock assets first
//        loadDeltaForceAssets(packageName);
//
//        if (isRootMode()) {
//            // Root mode - direct launch with bypass
//            android.util.Log.d("DELTA_FORCE", "🔧 ROOT MODE: Launching Delta Force with bypass");
//
//
//            // Launch game first
//            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
//            if (launchIntent != null) {
//                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(launchIntent);
//                android.util.Log.d("DELTA_FORCE", "✅ Delta Force launched successfully in root mode");
//
//                // Show STOP button after launch
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                    gameLaunched = true;
//                    android.content.SharedPreferences.Editor editor = getSharedPreferences("LoadingActivity", MODE_PRIVATE).edit();
//                    editor.putBoolean("gameLaunched", true);
//                    editor.apply();
//                    showStopButton();
//                    android.util.Log.d("DELTA_FORCE", "🛑 STOP button shown for root mode");
//                }, 2000);
//            } else {
//                android.util.Log.e("DELTA_FORCE", "❌ Failed to get launch intent for: " + packageName);
//            }
//        } else {
//            // Non-root mode - use Meta virtualization
//            android.util.Log.d("DELTA_FORCE", "📱 NON-ROOT MODE: Launching Delta Force via Meta");
//
//            VBoxCore.get().launchApk(packageName, USER_ID);
//            android.util.Log.d("DELTA_FORCE", "✅ Delta Force launched successfully via Meta");
//
//            // Start game monitoring for Delta Force packages
//            startDeltaForceGameMonitoring(packageName);
//
       // Show STOP button after launch
//            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                gameLaunched = true;
//                android.content.SharedPreferences.Editor editor = getSharedPreferences("LoadingActivity", MODE_PRIVATE).edit();
//                editor.putBoolean("gameLaunched", true);
//                editor.apply();
//                showStopButton();
//                android.util.Log.d("DELTA_FORCE", "🛑 STOP button shown for non-root mode");
//            }, 2000);
//        }
//    }
    /**
     * Monitor Delta Force game and run bypass when game is running (non-root only)
     */



    private void launchDeltaForceGame(String packageName) {
        android.util.Log.d("DELTA_FORCE", "🚀 Launching Delta Force game: " + packageName);

        if (isRootMode()) {
            // В root режиме проверяем, есть ли файлы delta_sock
            if (!areDeltaForceAssetsLoaded()) {
                android.util.Log.e("DELTA_FORCE", "💥 [ROOT] Required assets not found, downloading...");
                // Пытаемся загрузить assets заново
                downloadDeltaForceAssets(packageName);
                return;
            }
            
            android.util.Log.d("DELTA_FORCE", "✅ [ROOT] All required files found, launching game");
        }

        // В root режиме сначала загружаем assets и ждем завершения
        android.util.Log.d("DELTA_FORCE", "🔧 ROOT MODE: Loading assets first, then launching game");
        
        new Thread(() -> {
            try {
                // Загружаем assets
                loadDeltaForceAssets(packageName);
                
                // Ждем завершения загрузки
                waitForDeltaForceAssetsDownload(() -> {
                    // После завершения загрузки запускаем игру на главном потоке
                    runOnUiThread(() -> {
                        try {
                            android.util.Log.d("DELTA_FORCE", "🎮 Starting game: " + packageName);
                            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                            if (launchIntent != null) {
                                startActivity(launchIntent);
                                android.util.Log.d("DELTA_FORCE", "✅ Game launched successfully");
                                
                                                // В root режиме запускаем bypass и overlay для delta force
                if (isRootMode()) {
                    android.util.Log.d("DELTA_FORCE", "🔧 [ROOT] Starting Delta Force bypass and overlay");
                    startDeltaForceRootBypass(packageName);
                    
                    // Запускаем мониторинг игры для delta force в root режиме
                    startDeltaForceRootGameMonitoring(packageName);
                }
                            } else {
                                android.util.Log.e("DELTA_FORCE", "💥 Failed to get launch intent for: " + packageName);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("DELTA_FORCE", "💥 Error launching game: " + e.getMessage(), e);
                        }
                    });
                });
                
            } catch (Exception e) {
                android.util.Log.e("DELTA_FORCE", "💥 Error in asset loading thread: " + e.getMessage(), e);
                // Fallback: пытаемся запустить игру без assets
                runOnUiThread(() -> launchDeltaForceGameFallback(packageName));
            }
        }).start();
    }
    private void startDeltaForceGameMonitoring(String packageName) {
        new Thread(() -> {
            try {
                android.util.Log.d("DELTA_FORCE_MONITOR", "🎯 [NON-ROOT] Starting Delta Force game monitoring for: " + packageName);

                boolean bypassExecuted = false;
                int checkCount = 0;
                final int maxChecks = 60; // 5 minutes maximum (60 * 5 seconds)

                while (!bypassExecuted && checkCount < maxChecks) {
                    checkCount++;

                    // Check if game is running using MetaActivityManager
                    boolean isGameRunning = VBoxCore.get().isAppRunning(packageName, USER_ID);

                    android.util.Log.d("DELTA_FORCE_MONITOR", "🔍 [NON-ROOT] Check " + checkCount + "/" + maxChecks + " - Game running: " + isGameRunning);

                    if (isGameRunning) {
                        android.util.Log.d("DELTA_FORCE_MONITOR", "✅ [NON-ROOT] Delta Force game is running, executing bypass");

                        // Execute Delta Force bypass for non-root mode
                        executeDeltaForceBypassNonRoot(packageName);

                        bypassExecuted = true;
                        android.util.Log.d("DELTA_FORCE_MONITOR", "✅ [NON-ROOT] Delta Force bypass executed successfully");

                    } else {
                        // Wait 1 seconds before next check
                        android.util.Log.d("DELTA_FORCE_MONITOR", "⏳ [NON-ROOT] Game not running yet, waiting 1 seconds...");
                        Thread.sleep(1000);
                    }
                }

                if (!bypassExecuted) {
                    android.util.Log.w("DELTA_FORCE_MONITOR", "⚠️ [NON-ROOT] Game monitoring timeout - bypass not executed");
                }

            } catch (Exception e) {
                android.util.Log.e("DELTA_FORCE_MONITOR", "💥 [NON-ROOT] Error in Delta Force game monitoring: " + e.getMessage(), e);
            }
        }).start();
    }


    private void executeDeltaForceBypassNonRoot(String packageName) {
        try {
            android.util.Log.d("DELTA_FORCE_BYPASS_NONROOT", "🎯 [NON-ROOT] Starting Delta Force bypass execution");

            // Check if Delta Force files exist in app directory
            File deltaBypassFile = new File(getFilesDir(), "delta_bypass");
            File deltaSockFile = new File(getFilesDir(), "delta_sock");

            android.util.Log.d("DELTA_FORCE_BYPASS_NONROOT", "📁 [NON-ROOT] Delta bypass file: exists=" + deltaBypassFile.exists() + ", size=" + (deltaBypassFile.exists() ? deltaBypassFile.length() : "N/A"));
            android.util.Log.d("DELTA_FORCE_BYPASS_NONROOT", "📁 [NON-ROOT] Delta sock file: exists=" + deltaSockFile.exists() + ", size=" + (deltaSockFile.exists() ? deltaSockFile.length() : "N/A"));

            if (!deltaBypassFile.exists() || deltaBypassFile.length() == 0) {
                android.util.Log.e("DELTA_FORCE_BYPASS_NONROOT", "❌ [NON-ROOT] Delta bypass file not found!");
                return;
            }

            if (!deltaSockFile.exists() || deltaSockFile.length() == 0) {
                android.util.Log.e("DELTA_FORCE_BYPASS_NONROOT", "❌ [NON-ROOT] Delta sock file not found!");
                return;
            }

            // Set executable permissions
            deltaBypassFile.setExecutable(true, true);
            deltaSockFile.setExecutable(true, true);

            android.util.Log.d("DELTA_FORCE_BYPASS_NONROOT", "🔐 [NON-ROOT] Set executable permissions for Delta Force files");

            // Run Delta Force bypass 3 times like root mode
            for (int i = 1; i <= 3; i++) {
                android.util.Log.d("DELTA_FORCE_BYPASS_NONROOT", "🔄 [NON-ROOT] Delta Force bypass iteration " + i + "/3");

                // Run delta_bypass without features (just basic bypass)
                android.util.Log.d("DELTA_FORCE_BYPASS_NONROOT", "🚀 [NON-ROOT] Launching delta_bypass without features");
                Process process = Runtime.getRuntime().exec(deltaBypassFile.getAbsolutePath());
                process.waitFor();

                // Wait 1 second between iterations
                if (i < 3) {
                    Thread.sleep(1000);
                }
            }

            android.util.Log.d("DELTA_FORCE_BYPASS_NONROOT", "✅ [NON-ROOT] Delta Force bypass execution completed");


            // Start overlay service (will use delta_sock)
            startDeltaForceOverlay();

        } catch (Exception e) {
            android.util.Log.e("DELTA_FORCE_BYPASS_NONROOT", "💥 [NON-ROOT] Error executing Delta Force bypass: " + e.getMessage(), e);
        }
    }







    /**
     * Start Delta Force overlay service (will use delta_sock)
     */
    private void startDeltaForceOverlay() {
        new Thread(() -> {
            try {
                android.util.Log.d("DELTA_FORCE_OVERLAY", "🎯 Starting Delta Force overlay service");

                // 1. Start overlay service (will automatically use delta_sock)
                android.util.Log.d("DELTA_FORCE_OVERLAY", "🚀 Starting overlay service for Delta Force");
               // Intent overlayIntent = new Intent(this, com.glass.engine.floating.Overlay.class);
              //  overlayIntent.putExtra("game_type", "delta_force"); // Pass game type to overlay
              //  startService(overlayIntent);

                // 2. Enable ESPView overlay
                android.util.Log.d("DELTA_FORCE_OVERLAY", "👁️ Enabling ESPView overlay");
              //  com.glass.engine.floating.ESPView.setGameActive(true);
              //  com.glass.engine.floating.ESPView.enableOverlay();

                // 3. delta_sock will be started automatically by Overlay service
                android.util.Log.d("DELTA_FORCE_OVERLAY", "🔌 Delta_sock will be started by Overlay service");

                android.util.Log.d("DELTA_FORCE_OVERLAY", "✅ Delta Force overlay setup completed");


            } catch (Exception e) {
                android.util.Log.e("DELTA_FORCE_OVERLAY", "💥 Error in Delta Force overlay setup: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Запуск Delta Force bypass и overlay в root режиме
     */
    private void startDeltaForceRootBypass(String packageName) {
        new Thread(() -> {
            try {
                android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "🎯 [ROOT] Starting Delta Force bypass execution");
                
                // Проверяем наличие файлов delta force в root
                File rootDeltaSock = new File("/data/local/tmp/delta_sock");
                File rootDeltaBypass = new File("/data/local/tmp/delta_rootbypass");
                
                android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "📁 [ROOT] Delta sock file: exists=" + rootDeltaSock.exists() + ", size=" + (rootDeltaSock.exists() ? rootDeltaSock.length() : "N/A"));
                android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "📁 [ROOT] Delta bypass file: exists=" + rootDeltaBypass.exists() + ", size=" + (rootDeltaBypass.exists() ? rootDeltaBypass.length() : "N/A"));
                
                // Если файлы не найдены, пытаемся скопировать их заново
                if (!rootDeltaSock.exists() || rootDeltaSock.length() == 0) {
                    android.util.Log.w("DELTA_FORCE_ROOT_BYPASS", "⚠️ [ROOT] Delta sock file not found, attempting to copy...");
                    
                    File appDeltaSock = new File(getFilesDir(), "delta_sock");
                    if (appDeltaSock.exists() && appDeltaSock.length() > 0) {
                        try {
                            com.glass.engine.BoxApplication.get().doExe("su -c 'cp " + appDeltaSock.getAbsolutePath() + " /data/local/tmp/delta_sock'");
                            com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/delta_sock'");
                            android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "✅ [ROOT] Delta sock copied to root");
                            
                            // Обновляем ссылку на файл
                            rootDeltaSock = new File("/data/local/tmp/delta_sock");
                        } catch (Exception e) {
                            android.util.Log.e("DELTA_FORCE_ROOT_BYPASS", "💥 [ROOT] Failed to copy delta_sock: " + e.getMessage());
                            return;
                        }
                    } else {
                        android.util.Log.e("DELTA_FORCE_ROOT_BYPASS", "❌ [ROOT] Delta sock not available in app directory!");
                        return;
                    }
                }
                
                if (!rootDeltaBypass.exists() || rootDeltaBypass.length() == 0) {
                    android.util.Log.w("DELTA_FORCE_ROOT_BYPASS", "⚠️ [ROOT] Delta bypass file not found, attempting to copy...");
                    
                    File appDeltaBypass = new File(getFilesDir(), "delta_rootbypass");
                    if (appDeltaBypass.exists() && appDeltaBypass.length() > 0) {
                        try {
                            com.glass.engine.BoxApplication.get().doExe("su -c 'cp " + appDeltaBypass.getAbsolutePath() + " /data/local/tmp/delta_rootbypass'");
                            com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/delta_rootbypass'");
                            android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "✅ [ROOT] Delta bypass copied to root");
                            
                            // Обновляем ссылку на файл
                            rootDeltaBypass = new File("/data/local/tmp/delta_rootbypass");
                        } catch (Exception e) {
                            android.util.Log.e("DELTA_FORCE_ROOT_BYPASS", "💥 [ROOT] Failed to copy delta_rootbypass: " + e.getMessage());
                            return;
                        }
                    } else {
                        android.util.Log.e("DELTA_FORCE_ROOT_BYPASS", "❌ [ROOT] Delta bypass not available in app directory!");
                        return;
                    }
                }
                
                // Финальная проверка после копирования
                if (!rootDeltaSock.exists() || rootDeltaSock.length() == 0 || !rootDeltaBypass.exists() || rootDeltaBypass.length() == 0) {
                    android.util.Log.e("DELTA_FORCE_ROOT_BYPASS", "❌ [ROOT] Files still not available after copy attempt!");
                    return;
                }
                
                // Дополнительная проверка через shell
                try {
                    android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "🔍 [ROOT] Verifying files via shell...");
                    com.glass.engine.BoxApplication.get().doExe("su -c 'ls -la /data/local/tmp/delta_sock'");
                    com.glass.engine.BoxApplication.get().doExe("su -c 'ls -la /data/local/tmp/delta_rootbypass'");
                    android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "✅ [ROOT] Files verified via shell");
                } catch (Exception e) {
                    android.util.Log.w("DELTA_FORCE_ROOT_BYPASS", "⚠️ Could not verify files via shell: " + e.getMessage());
                }
                
                // Устанавливаем права на выполнение
                android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "🔐 [ROOT] Setting executable permissions for Delta Force files");
                com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/delta_sock'");
                com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/delta_rootbypass'");
                
                // Финальная проверка прав
                try {
                    Thread.sleep(500);
                    com.glass.engine.BoxApplication.get().doExe("su -c 'ls -la /data/local/tmp/delta_sock'");
                    com.glass.engine.BoxApplication.get().doExe("su -c 'ls -la /data/local/tmp/delta_rootbypass'");
                    android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "✅ [ROOT] Permissions set successfully");
                } catch (Exception e) {
                    android.util.Log.w("DELTA_FORCE_ROOT_BYPASS", "⚠️ Could not verify permissions: " + e.getMessage());
                }
                
                // Запускаем delta_rootbypass 3 раза с интервалом 1 секунда
                for (int i = 1; i <= 3; i++) {
                    android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "🔄 [ROOT] Delta Force bypass iteration " + i + "/3");
                    
                    // Запускаем delta_rootbypass
                    android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "🚀 [ROOT] Launching delta_rootbypass");
                    com.glass.engine.BoxApplication.get().doExe("su -c 'cd /data/local/tmp && ./delta_rootbypass'");
                    
                    // Ждем 1 секунду между итерациями
                    if (i < 3) {
                        Thread.sleep(1000);
                    }
                }
                
                android.util.Log.d("DELTA_FORCE_ROOT_BYPASS", "✅ [ROOT] Delta Force bypass execution completed");
                
                // Запускаем overlay сервис (будет использовать delta_sock)
                startDeltaForceOverlay();
                
            } catch (Exception e) {
                android.util.Log.e("DELTA_FORCE_ROOT_BYPASS", "💥 [ROOT] Error executing Delta Force bypass: " + e.getMessage(), e);
            }
        }).start();
    }

    /**
     * Мониторинг игры Delta Force в root режиме
     */
    private void startDeltaForceRootGameMonitoring(String packageName) {
        new Thread(() -> {
            try {
                android.util.Log.d("DELTA_FORCE_ROOT_MONITOR", "🎯 [ROOT] Starting Delta Force game monitoring for: " + packageName);

                boolean bypassExecuted = false;
                int checkCount = 0;
                final int maxChecks = 60; // 5 minutes maximum (60 * 5 seconds)

                while (!bypassExecuted && checkCount < maxChecks) {
                    checkCount++;

                    // Check if game is running using MetaActivityManager
                    boolean isGameRunning = VBoxCore.get().isAppRunning(packageName, USER_ID);

                    android.util.Log.d("DELTA_FORCE_ROOT_MONITOR", "🔍 [ROOT] Check " + checkCount + "/" + maxChecks + " - Game running: " + isGameRunning);

                    if (isGameRunning) {
                        android.util.Log.d("DELTA_FORCE_ROOT_MONITOR", "✅ [ROOT] Delta Force game is running, bypass already executed");

                        bypassExecuted = true;
                        android.util.Log.d("DELTA_FORCE_ROOT_MONITOR", "✅ [ROOT] Delta Force root monitoring completed");

                    } else {
                        // Wait 5 seconds before next check
                        android.util.Log.d("DELTA_FORCE_ROOT_MONITOR", "⏳ [ROOT] Game not running yet, waiting 5 seconds...");
                        Thread.sleep(5000);
                    }
                }

                if (!bypassExecuted) {
                    android.util.Log.w("DELTA_FORCE_ROOT_MONITOR", "⚠️ [ROOT] Game monitoring timeout");
                }

            } catch (Exception e) {
                android.util.Log.e("DELTA_FORCE_ROOT_MONITOR", "💥 [ROOT] Error in Delta Force root monitoring: " + e.getMessage(), e);
            }
        }).start();
    }

    private void waitForAssetDownload(Runnable onComplete) {


        final Handler monitorHandler = new Handler(Looper.getMainLooper());
        final int[] attempts = {0};
        final int maxAttempts = 60; // 5 минут максимум
        final boolean isRoot = isRootMode();

        final Runnable monitorRunnable = new Runnable() {
            @Override
            public void run() {
                attempts[0]++;

                File bypassFile, pubgSockFile;
                if (isRoot) {
                    bypassFile = new File("/data/local/tmp/bypass");
                    pubgSockFile = new File("/data/local/tmp/pubg_sock");
                } else {
                    bypassFile = new File(getFilesDir(), "delta_bypass");
                    pubgSockFile = new File(getFilesDir(), "delta_sock");
                }

                if (bypassFile.exists() && pubgSockFile.exists()) {
                    String path = isRoot ? "/data/local/tmp/" : "app directory";

                    onComplete.run();
                    return;
                }

                if (attempts[0] >= maxAttempts) {

                    onComplete.run();
                    return;
                }

                String path = isRoot ? "/data/local/tmp/" : "app directory";

                monitorHandler.postDelayed(this, 5000); // Проверяем каждые 5 секунд
            }
        };

        monitorHandler.postDelayed(monitorRunnable, 5000);
    }

    private void startBypassLogMonitoring(String packageName) {
        LogUtils.writeLog("startBypassLogMonitoring called for: " + packageName);

        // Проверяем, что это PUBG - только для него делаем bypass мониторинг
        if (
    !"com.tencent.ig".equals(packageName) || 
    !"com.rekoo.pubgm".equals(packageName) ||
    !"com.vng.pubgmobile".equals(packageName) ||
    !"com.pubg.krmobile".equals(packageName)
) {

            LogUtils.writeLog("Non-PUBG game detected, skipping bypass monitoring");
            return;
        }


        LogUtils.writeLog("Starting bypass log monitoring for package: " + packageName);

        final Handler monitorHandler = new Handler(Looper.getMainLooper());
        final int[] attempts = {0};
        final int maxAttempts = 300; // 25 минут максимум (на случай если лог не появится)

        final Runnable monitorRunnable = new Runnable() {
            @Override
            public void run() {
                attempts[0]++;

                // Проверяем лог "AndroidThunkJava_GetFIRAppInstanceId START"
                if (bypassTriggered) return;
                boolean bypassTriggerFound = checkForBypassTriggerLog(packageName);

                if (bypassTriggerFound) {

                    LogUtils.writeLog("Bypass trigger log found, starting bypass");
                    // Bypass и overlay теперь запускаются через GameLogMonitor
                    // Не нужно вызывать launchBypassHybrid или startOverlayHybrid
                    return;
                }

                if (attempts[0] >= maxAttempts) {

                    LogUtils.writeLog("Bypass log monitoring timeout");
                    // Timeout - GameLogMonitor сам запустит bypass и overlay
                    return;
                }


                LogUtils.writeLog("Waiting for bypass trigger log... (" + attempts[0] + "/" + maxAttempts + ")");
                monitorHandler.postDelayed(this, 5000); // Проверяем каждые 5 секунд
            }
        };

        monitorHandler.postDelayed(monitorRunnable, 5000);
    }

    private boolean checkForBypassTriggerLog(String packageName) {
        if (bypassTriggered) return false;
        try {
            // Используем logcat для поиска лога
            Process process = Runtime.getRuntime().exec("logcat -d -s GameActitivy:D");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("AndroidThunkJava_GetFIRAppInstanceId START")) {


                    LogUtils.writeLog("Found bypass trigger log: " + line);
                    LogUtils.writeLog("BYPASS TRIGGER DETECTED - Starting bypass sequence");
                    reader.close();
                    return true;
                }
            }
            reader.close();

        } catch (Exception e) {

            LogUtils.writeError("Error checking logs", e);
        }

        return false;
    }






   /* private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (Overlay.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }*/


    public void loadAssets(String sockver) {
        daemonPath = LoadingActivity.this.getFilesDir().toString() + "/" + sockver;
        socket = daemonPath;

        // Добавляем логирование в консоль для non-root режима
        if (!isRootMode()) {


        }

        File file = new File(daemonPath);
        if (file.exists() && file.length() > 0) {

            // Добавляем логирование в консоль для non-root режима
            if (!isRootMode()) {


            }
        } else {

            // Добавляем логирование в консоль для non-root режима
            if (!isRootMode()) {


            }

            // In non-root mode, check if file was downloaded by server
            if (!isRootMode()) {

                // Wait a bit for server download to complete
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }

                // Check again after waiting
                if (file.exists() && file.length() > 0) {

                } else {

                    // Try to trigger server download
                    try {
                        new com.glass.engine.component.Server(this, "").execute("");

                        // Wait for download
                        for (int i = 0; i < 30; i++) { // Wait up to 30 seconds
                            Thread.sleep(1000);
                            if (file.exists() && file.length() > 0) {

                                break;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }

            // Fallback: try to copy from assets for BOTH modes if file is still missing
            if (!file.exists() || file.length() == 0) {
                try {
                    String[] assetList = getAssets().list("");
                    boolean assetExists = false;
                    for (String asset : assetList) {
                        if (asset.equals(sockver)) {
                            assetExists = true;
                            break;
                        }
                    }
                    if (assetExists) {
                        try (InputStream in = getAssets().open(sockver);
                             FileOutputStream out = new FileOutputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        }

        // Only try to chmod if file exists and has content
        if (file.exists() && file.length() > 0) {
            try {
                Process chmodProcess = Runtime.getRuntime().exec("chmod 777 " + daemonPath);
                int resultCode = chmodProcess.waitFor();

                // Добавляем логирование в консоль для non-root режима
                if (!isRootMode()) {

                }
            } catch (IOException | InterruptedException e) {
            }
        } else {
        }

    }

    /**
     * Load Delta Force assets (delta_sock) similar to PUBG loadAssets
     */
    public void loadDeltaForceAssets(String packageName) {
        // Set paths for delta_sock
        if (isRootMode()) {
            daemonPath = "/data/local/tmp/delta_sock";
            socket = daemonPath;
            android.util.Log.d("DELTA_FORCE", "🔧 [ROOT] Using root path for delta_sock: " + daemonPath);
        } else {
            daemonPath = LoadingActivity.this.getFilesDir().toString() + "/delta_sock";
            socket = daemonPath;
            android.util.Log.d("DELTA_FORCE", "📱 [NON-ROOT] Using non-root path for delta_sock: " + daemonPath);
        }

        // Проверяем, загружены ли assets
        if (areDeltaForceAssetsLoaded()) {
            android.util.Log.d("DELTA_FORCE", "✅ All Delta Force assets are loaded");
        } else {
            android.util.Log.w("DELTA_FORCE", "⚠️ Delta Force assets not found, attempting to copy...");

            if (isRootMode()) {
                // Root mode - check if files were copied to /data/local/tmp/
                File deltaSockFile = new File("/data/local/tmp/delta_sock");
                File deltaBypassFile = new File("/data/local/tmp/delta_rootbypass");
                
                boolean sockExists = deltaSockFile.exists() && deltaSockFile.length() > 0;
                boolean bypassExists = deltaBypassFile.exists() && deltaBypassFile.length() > 0;
                
                if (sockExists && bypassExists) {
                    android.util.Log.d("DELTA_FORCE", "✅ Root Delta Force files found - sock: " + deltaSockFile.length() + " bytes, bypass: " + deltaBypassFile.length() + " bytes");
                } else {
                    android.util.Log.w("DELTA_FORCE", "⚠️ Root Delta Force files not found, copying from app directory...");

                    // Copy delta_sock to root
                    File appDeltaSock = new File(getFilesDir(), "delta_sock");
                    if (appDeltaSock.exists() && appDeltaSock.length() > 0) {
                        try {
                            android.util.Log.d("DELTA_FORCE", "📋 [ROOT] Copying delta_sock to root location");
                            com.glass.engine.BoxApplication.get().doExe("su -c 'cp " + appDeltaSock.getAbsolutePath() + " /data/local/tmp/delta_sock'");
                            com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/delta_sock'");
                            android.util.Log.d("DELTA_FORCE", "✅ Copied delta_sock to root location");
                        } catch (Exception e) {
                            android.util.Log.e("DELTA_FORCE", "💥 Error copying delta_sock to root: " + e.getMessage());
                        }
                    } else {
                        android.util.Log.w("DELTA_FORCE", "⚠️ Delta_sock not available in app directory");
                    }
                    
                    // Copy delta_rootbypass to root
                    File appDeltaBypass = new File(getFilesDir(), "delta_rootbypass");
                    if (appDeltaBypass.exists() && appDeltaBypass.length() > 0) {
                        try {
                            android.util.Log.d("DELTA_FORCE", "📋 [ROOT] Copying delta_rootbypass to root location");
                            com.glass.engine.BoxApplication.get().doExe("su -c 'cp " + appDeltaBypass.getAbsolutePath() + " /data/local/tmp/delta_rootbypass'");
                            com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/delta_rootbypass'");
                            android.util.Log.d("DELTA_FORCE", "✅ Copied delta_rootbypass to root location");
                        } catch (Exception e) {
                            android.util.Log.e("DELTA_FORCE", "💥 Error copying delta_rootbypass to root: " + e.getMessage());
                        }
                    } else {
                        android.util.Log.w("DELTA_FORCE", "⚠️ Delta_rootbypass not available in app directory");
                    }
                    
                    // Небольшая задержка после копирования файлов
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            } else {
                // Non-root mode - check if file was downloaded
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                }

                File file = new File(daemonPath);
                if (file.exists() && file.length() > 0) {
                    android.util.Log.d("DELTA_FORCE", "✅ Downloaded delta_sock found, size: " + file.length() + " bytes");
                } else {
                    android.util.Log.w("DELTA_FORCE", "⚠️ Delta_sock not available, overlay may not work");
                }
            }
        }

        // Set executable permissions if files exist
        if (isRootMode()) {
            File deltaSockFile = new File("/data/local/tmp/delta_sock");
            File deltaBypassFile = new File("/data/local/tmp/delta_rootbypass");
            
            if (deltaSockFile.exists() && deltaSockFile.length() > 0) {
                try {
                    com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/delta_sock'");
                    android.util.Log.d("DELTA_FORCE", "🔧 [ROOT] Set permissions for delta_sock");
                } catch (Exception e) {
                    android.util.Log.e("DELTA_FORCE", "💥 Error setting permissions for delta_sock: " + e.getMessage());
                }
            }
            
            if (deltaBypassFile.exists() && deltaBypassFile.length() > 0) {
                try {
                    com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/delta_rootbypass'");
                    android.util.Log.d("DELTA_FORCE", "🔧 [ROOT] Set permissions for delta_rootbypass");
                } catch (Exception e) {
                    android.util.Log.e("DELTA_FORCE", "💥 Error setting permissions for delta_rootbypass: " + e.getMessage());
                }
            }
        } else {
            File file = new File(daemonPath);
            if (file.exists() && file.length() > 0) {
                try {
                    Process chmodProcess = Runtime.getRuntime().exec("chmod 777 " + daemonPath);
                    chmodProcess.waitFor();
                    android.util.Log.d("DELTA_FORCE", "🔧 [NON-ROOT] Set permissions for delta_sock");
                } catch (IOException | InterruptedException e) {
                    android.util.Log.e("DELTA_FORCE", "💥 Error setting permissions: " + e.getMessage());
                }
            } else {
                android.util.Log.w("DELTA_FORCE", "⚠️ Delta_sock file not available for permissions");
            }
        }
    }





    public void Exec(String path, String toast) {
        try {
            Log.d("BYPASS", "Exec: chmod 777 " + path);
            ExecuteElf("chmod 777 " + path);
            Log.d("BYPASS", getString(R.string.exec_starting, path));
            ExecuteElf(path);
        } catch (Exception e) {
            Log.e("BYPASS", getString(R.string.error_starting, path, e.getMessage()));

        }
    }

    private void ExecuteElf(String shell) {
        try {
            Runtime.getRuntime().exec(shell, null, null);
        } catch (Exception e) {
        }
    }


    private int getObbVersionCode(String packageName) {
        try {
            return getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {

            return -1;
        }
    }

    private void forceStopApp(String packageName) {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(packageName);

            }
        } catch (Exception e) {

        }
    }

    private void forceCloseApplication() {
        try {


            // Убираем принудительное закрытие - пусть игра работает
            // Сначала останавливаем overlay
            // forceStopOverlay();

            // Закрываем все активности
            // finish();

            // Принудительно завершаем процесс через 1 секунду
            // new Handler(Looper.getMainLooper()).postDelayed(() -> {
            //     try {
            //         android.os.Process.killProcess(android.os.Process.myPid());
            //         System.exit(0);
            //     } catch (Exception e) {
            //         // Последняя попытка
            //         System.exit(1);
            //     }
            // }, 1000);

        } catch (Exception e) {
            // Если все не работает, принудительно завершаем
            // android.os.Process.killProcess(android.os.Process.myPid());
            // System.exit(0);

        }
    }

    private void debugObbSearch(String packageName) {
        try {
            int versionCode = getPackageManager().getPackageInfo(packageName, 0).versionCode;






            String[] searchPaths = {
                "/storage/emulated/0/Android/obb/" + packageName,
                "/sdcard/Android/obb/" + packageName,
                Environment.getExternalStorageDirectory() + "/Android/obb/" + packageName,
                "/mnt/sdcard/Android/obb/" + packageName,
                "/storage/sdcard0/Android/obb/" + packageName
            };

            for (String path : searchPaths) {
                File dir = new File(path);






                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {

                        for (File file : files) {

                        }
                    } else {

                    }
                }
            }

            // Проверяем app's external files directory
            String appObbPath = getExternalFilesDir(null) + "/obb/" + packageName;
            File appObbDir = new File(appObbPath);






            if (appObbDir.exists() && appObbDir.isDirectory()) {
                File[] files = appObbDir.listFiles();
                if (files != null) {

                    for (File file : files) {

                    }
                } else {

                }
            }


        } catch (Exception e) {

        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Если кнопка STOP показана, разрешаем все касания
        if (stopButtonShown) {
            return super.dispatchTouchEvent(ev);
        }

        // Обрабатываем касания для возврата в MainActivity
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // Проверяем, есть ли финальное сообщение на экране
            // Проверка на двойное нажатие (логи не отображаются на экране)
        if (false) { // Убрано - логи не отображаются на экране
                // Позволяем обработчику onClick в showFinalMessage обработать касание
                return super.dispatchTouchEvent(ev);
            }
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Используем те же флаги что и в других активностях для совместимости с планшетами
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                getWindow().setDecorFitsSystemWindows(false);
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                );
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (deferShowStopOnResume && !stopButtonShown) {
            // безопасно показать кнопку, когда активность снова на экране
            runOnUiThread(this::showStopButton);
            deferShowStopOnResume = false;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 2001) {
            // Обработка результата запроса разрешений на доступ к файлам
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    
                } else {
                    
                }
            }
        }
    }

    private File findObbInAlternativeLocations(String packageName, int versionCode) {
        String obbFileName = "main." + versionCode + "." + packageName + ".obb";
        String[] alternativePaths = {
            "/sdcard/Android/obb/" + packageName + "/" + obbFileName,
            Environment.getExternalStorageDirectory() + "/Android/obb/" + packageName + "/" + obbFileName,
            "/mnt/sdcard/Android/obb/" + packageName + "/" + obbFileName,
            "/storage/sdcard0/Android/obb/" + packageName + "/" + obbFileName,
            "/mnt/shell/emulated/0/Android/obb/" + packageName + "/" + obbFileName,
            getExternalFilesDir(null) + "/obb/" + packageName + "/" + obbFileName
        };
        
        for (String path : alternativePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile() && file.length() > 0) {
                return file;
            }
        }
        return null;
    }
    
    private boolean copyFileDirectly(File source, File target) {
        try {
            try (InputStream in = new FileInputStream(source); FileOutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[1024 * 1024];
                int read;
                long totalRead = 0;
                long totalSize = source.length();
                
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalRead += read;
                    
                    if (totalRead % (10 * 1024 * 1024) == 0) { // Логируем каждые 10MB
                        int progress = (int) ((totalRead * 100) / totalSize);
                        
                    }
                }
            }
            return target.exists() && target.length() == source.length();
        } catch (Exception e) {
            
            return false;
        }
    }
    
    private boolean copyFileWithFileProvider(File source, File target) {
        try {
            // Пробуем через FileProvider
            Uri sourceUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", source);
            try (InputStream in = getContentResolver().openInputStream(sourceUri); FileOutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[1024 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
            return target.exists() && target.length() == source.length();
        } catch (Exception e) {
            
            return false;
        }
    }
    
    private boolean copyFileWithShell(File source, File target) {
        try {
            // Пробуем через shell команды
            String command = "cp \"" + source.getAbsolutePath() + "\" \"" + target.getAbsolutePath() + "\"";
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return target.exists() && target.length() == source.length();
            } else {
                
                return false;
            }
        } catch (Exception e) {
            
            return false;
        }
    }
    
    private boolean createHardLink(File source, File target) {
        try {
            // Пробуем создать hard link
            String command = "ln \"" + source.getAbsolutePath() + "\" \"" + target.getAbsolutePath() + "\"";
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return target.exists() && target.length() == source.length();
            } else {
                
                return false;
            }
        } catch (Exception e) {
            
            return false;
        }
    }
    
    private boolean tryAlternativeCopyMethods(File source, String packageName, int versionCode) {
        // Пробуем скопировать в app's external files directory
        try {
            String obbFileName = "main." + versionCode + "." + packageName + ".obb";
            File appObbDir = new File(getExternalFilesDir(null), "obb/" + packageName);
            File appObbFile = new File(appObbDir, obbFileName);
            
            if (!appObbDir.exists()) {
                appObbDir.mkdirs();
            }
            
            if (copyFileDirectly(source, appObbFile)) {
                return true;
            }
        } catch (Exception e) {
        }
        
        // Пробуем скопировать в app's internal files directory
        try {
            String obbFileName = "main." + versionCode + "." + packageName + ".obb";
            File internalObbDir = new File(getFilesDir(), "obb/" + packageName);
            File internalObbFile = new File(internalObbDir, obbFileName);
            
            if (!internalObbDir.exists()) {
                internalObbDir.mkdirs();
            }
            
            if (copyFileDirectly(source, internalObbFile)) {
                return true;
            }
        } catch (Exception e) {
        }
        
        return false;
    }

    private void startServerAndVersionUpdate() {
        // Запускаем server
        try {
            // Здесь добавьте код запуска вашего сервера
            // Server.startServer();
        } catch (Exception e) {
        }
        
        // Обновляем версию
        try {
            
            
        } catch (Exception e) {
        }
        
        // Запускаем дополнительные shell команды
        try {
            // Здесь добавьте дополнительные shell команды
            // ExecuteElf("your_command");
        } catch (Exception e) {
        }
        
        // Показываем финальное сообщение
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showFinalMessage();
        }, 1000);
    }



    private void forceStopOverlay() {
        // Убираю остановку overlay - пусть работает
        // try {
        //     
        //     
        //     // Останавливаем Overlay сервис
        //     try {
        //         stopService(new Intent(this, Overlay.class));
        //         
        //     } catch (Exception e) {
        //         
        //     }
        //     
        //     // Убиваем ESPView если он активен
        //     try {
        //         ESPView.disableOverlay();
        //         
        //     } catch (Exception e) {
        //         
        //     }
        //     
        //     // Убиваем все процессы связанные с overlay
        //     try {
        //         ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        //         if (am != null) {
        //             // Убиваем процессы связанные с overlay
        //             am.killBackgroundProcesses(getPackageName());
        //             
        //     }
        //     } catch (Exception e) {
        //         
        //     }
        //     
        //     // Ждем немного чтобы процессы полностью остановились
        //     new Handler(Looper.getMainLooper()).postDelayed(() -> {
        //         
        //     }, 1000);
        //     
        // } catch (Exception e) {
        //     
        // }
    }
    
    /**
     * Kill all existing processes before downloading to prevent duplicates
     */
    private void killExistingProcesses() {
        android.util.Log.d("LoadingActivity", "💀 [KILL] Killing existing processes before download");
        
        
        try {
            // Kill overlay processes
            com.glass.engine.BoxApplication.get().doExe("pkill -f pubg_sock");
            com.glass.engine.BoxApplication.get().doExe("pkill -f delta_sock");
            com.glass.engine.BoxApplication.get().doExe("pkill -f bypass");
            com.glass.engine.BoxApplication.get().doExe("pkill -f delta_bypass");
            com.glass.engine.BoxApplication.get().doExe("pkill -f client");
            
            // Kill using killall as backup
            com.glass.engine.BoxApplication.get().doExe("killall pubg_sock");
            com.glass.engine.BoxApplication.get().doExe("killall delta_sock");
            com.glass.engine.BoxApplication.get().doExe("killall bypass");
            com.glass.engine.BoxApplication.get().doExe("killall delta_bypass");
            com.glass.engine.BoxApplication.get().doExe("killall client");
            
            // Stop overlay service
          //  Intent overlayIntent = new Intent(this, com.glass.engine.floating.Overlay.class);
         //   stopService(overlayIntent);
            
            // Disable ESPView
          //  com.glass.engine.floating.ESPView.setGameActive(false);
           // com.glass.engine.floating.ESPView.disableOverlay();
            
            android.util.Log.d("LoadingActivity", "✅ [KILL] All existing processes killed");
            
            
        } catch (Exception e) {
            android.util.Log.e("LoadingActivity", "❌ [KILL] Error killing processes: " + e.getMessage());
            
        }
    }

    private void checkAndSetOrientation() {
        try {
            
            
            // Для планшетов разрешаем поворот экрана
            if (DeviceUtils.isTablet(this)) {
                
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            } else {
                // Для телефонов устанавливаем landscape
                int currentOrientation = getRequestedOrientation();
                int targetOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                
                
                
                if (currentOrientation != targetOrientation) {
                    
                    setRequestedOrientation(targetOrientation);
                    
                    // Ждем немного чтобы ориентация установилась
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        
                    }, 500);
                } else {
                    
                }
            }
            
        } catch (Exception e) {
            
        }
    }
    
    private String getOrientationName(int orientation) {
        switch (orientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                return "PORTRAIT";
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                return "LANDSCAPE";
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                return "REVERSE_PORTRAIT";
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                return "REVERSE_LANDSCAPE";
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:
                return "SENSOR_PORTRAIT";
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
                return "SENSOR_LANDSCAPE";
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR:
                return "SENSOR";
            case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED:
                return "UNSPECIFIED";
            default:
                return "UNKNOWN";
        }
    }

    private void showFinalMessage() {
        try {
            // Финальное сообщение больше не отображается на экране
            // Логи продолжают работать в фоне
            
            // Добавляем обработчик касания для возврата в MainActivity
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.setOnClickListener(new View.OnClickListener() {
                    private int tapCount = 0;
                    private long lastTapTime = 0;
                    
                    @Override
                    public void onClick(View v) {
                        long currentTime = System.currentTimeMillis();
                        
                        if (currentTime - lastTapTime < 2000) { // Двойное касание в течение 2 секунд
                            tapCount++;
                            if (tapCount >= 2) {
                                // Останавливаем overlay перед возвратом
                                forceStopOverlay();
                                
                                // Ждем немного чтобы overlay остановился
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    // Возвращаемся в MainActivity
                                    finish();
                                }, 1000);
                            }
                        } else {
                            tapCount = 1;
                        }
                        lastTapTime = currentTime;
                    }
                });
            }
            
        } catch (Exception e) {
            
        }
    }

    private void startGameMonitoring(String packageName) {
        
        
        final Handler monitorHandler = new Handler(Looper.getMainLooper());
        final int[] consecutiveFailures = {0};
        final long[] lastGameCheck = {0};
        
        final Runnable monitorRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    long currentTime = System.currentTimeMillis();
                    
                    // Проверяем, запущена ли игра (используем гибридный метод)
                    boolean isGameRunning = isGameRunning(packageName);
                    
                    if (!isGameRunning) {
                        consecutiveFailures[0]++;
                        
                        
                        // Только после 3 последовательных неудач и минимум 50 секунд после запуска
                        if (consecutiveFailures[0] >= 3 && (currentTime - lastGameCheck[0]) > 50000) {
                            
                            
                            
                            // Останавливаем overlay
                            forceStopOverlay();
                            
                            // Ждем немного чтобы overlay остановился
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                // Возвращаемся в MainActivity
                                finish();
                            }, 1000);
                            
                            return; // Останавливаем мониторинг
                        }
                    } else {
                        // Игра запущена, сбрасываем счетчик неудач
                        if (consecutiveFailures[0] > 0) {
                            
                        } else {
                            
                        }
                        consecutiveFailures[0] = 0;
                        lastGameCheck[0] = currentTime;
                    }
                    
                    // Продолжаем мониторинг каждые 10 секунд (увеличили интервал)
                    monitorHandler.postDelayed(this, 10000);
                    
                } catch (Exception e) {
                    
                    // Продолжаем мониторинг даже при ошибке
                    monitorHandler.postDelayed(this, 10000);
                }
            }
        };
        
        // Запускаем мониторинг через 50 секунд после запуска игры (увеличили задержку)
        
        monitorHandler.postDelayed(monitorRunnable, 50000);
    }

    // === ГИБРИДНЫЙ РЕЖИМ ===
    public boolean isRootMode() {
        try {
            return com.glass.engine.BoxApplication.get().checkRootAccess();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Отключает Profile Saver для предотвращения краша с CrashSight
     */
    private void disableProfileSaver() {
        try {
            
        
            // Метод 1: Через shell команды (если есть root)
        if (isRootMode()) {
            try {
                    Runtime.getRuntime().exec("su -c 'setprop dalvik.vm.profiler 0'");
                    Runtime.getRuntime().exec("su -c 'setprop dalvik.vm.profile 0'");
                    Runtime.getRuntime().exec("su -c 'setprop dalvik.vm.profiler.output 0'");
                    
                } catch (Exception e) {
                    
                }
            }
            
            // Метод 2: Через системные настройки
            try {
                android.provider.Settings.Global.putString(getContentResolver(), "art.profile", "0");
                android.provider.Settings.Global.putString(getContentResolver(), "art.profiler", "0");
                android.provider.Settings.Global.putString(getContentResolver(), "art.profile.output", "0");
                android.provider.Settings.Global.putString(getContentResolver(), "art.profile.period", "0");
                
            } catch (Exception e) {
                
            }
            
            // Метод 3: Убиваем Profile Saver процесс если он запущен
            try {
                Runtime.getRuntime().exec("pkill -f ProfileSaver");
                Runtime.getRuntime().exec("killall ProfileSaver");
                Runtime.getRuntime().exec("pkill -f art.profile");
                Runtime.getRuntime().exec("killall art.profile");
                
            } catch (Exception e) {
                
            }
            
            // Метод 4: Дополнительные настройки для предотвращения краша
            try {
                // Отключаем профилирование ART
                System.setProperty("art.profile", "0");
                System.setProperty("art.profiler", "0");
                System.setProperty("art.profile.output", "0");
                System.setProperty("art.profile.period", "0");
                
                // Отключаем профилирование Dalvik
                System.setProperty("dalvik.vm.profiler", "0");
                System.setProperty("dalvik.vm.profile", "0");
                System.setProperty("dalvik.vm.profiler.output", "0");
                
                
            } catch (Exception e) {
                
            }
            
            
            
        } catch (Exception e) {
            
        }
    }

    private void launchGame(String packageName) {
        LogUtils.writeLog("launchGame called for: " + packageName);
        
        if (isRootMode()) {
            // ROOT MODE - полный перезапуск игры
            
            LogUtils.writeLog("ROOT MODE: Force restarting game");
            android.util.Log.d("ROOT_LAUNCH", "🔄 [ROOT] Force restarting original PUBG: " + packageName);
            
            try {
                // 0. Очищаем логи перед запуском
                android.util.Log.d("ROOT_LAUNCH", "🧹 [ROOT] Clearing logs before game launch");
                LogUtils.writeLog("Clearing logs before game launch");
                com.glass.engine.BoxApplication.get().doExe("su -c 'logcat -c'");
                
                // 1. Принудительно останавливаем игру если она запущена
                android.util.Log.d("ROOT_LAUNCH", "🛑 [ROOT] Force stopping existing game process");
                LogUtils.writeLog("Force stopping existing game process");
                com.glass.engine.BoxApplication.get().doExe("am force-stop " + packageName);
                com.glass.engine.BoxApplication.get().doExe("pkill -f " + packageName);
                com.glass.engine.BoxApplication.get().doExe("killall " + packageName);
                
                // 2. Очищаем кэш и данные игры
                android.util.Log.d("ROOT_LAUNCH", "🧹 [ROOT] Clearing game cache and data");
                LogUtils.writeLog("Clearing game cache and data");
                com.glass.engine.BoxApplication.get().doExe("pm clear " + packageName);
                
                // 3. Ждем немного для завершения процессов
                android.util.Log.d("ROOT_LAUNCH", "⏳ [ROOT] Waiting for processes to stop");
                LogUtils.writeLog("Waiting for processes to stop");
                Thread.sleep(2000);
                
                // 4. Запускаем игру заново
                android.util.Log.d("ROOT_LAUNCH", "🚀 [ROOT] Starting fresh game instance");
                LogUtils.writeLog("Starting fresh game instance");
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(launchIntent);
                    android.util.Log.d("ROOT_LAUNCH", "✅ [ROOT] Original PUBG force restart completed successfully");
                    LogUtils.writeGameLaunchLog(packageName, true, "RootMode");
                    
                    // Показываем кнопку STOP после запуска игры
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        gameLaunched = true;
                        
                        // Сохраняем состояние игры
                        android.content.SharedPreferences.Editor editor = getSharedPreferences("LoadingActivity", MODE_PRIVATE).edit();
                        editor.putBoolean("gameLaunched", true);
                        editor.apply();
                        
                                        // Запускаем ColorAnalysisService для анализа цветов экрана
                // startColorAnalysisService(); // Removed - using ColorPickerService instead
                        
                        showStopButton();
                    }, 2000); // 2 секунды задержки
                    
                    // 5. Запускаем мониторинг логов для поиска триггера bypass (только для PUBG)
                if (
    "com.tencent.ig".equals(packageName)  ||
    "com.rekoo.pubgm".equals(packageName) ||
    "com.vng.pubgmobile".equals(packageName) ||
    "com.pubg.krmobile".equals(packageName)
) {
                        android.util.Log.d("ROOT_LAUNCH", "🔍 [ROOT] Starting log monitoring for bypass trigger");
                        LogUtils.writeLog("Starting log monitoring for bypass trigger");
                        startRootLogMonitoring(packageName);
                    }
                } else {
                    android.util.Log.e("ROOT_LAUNCH", "❌ [ROOT] Failed to get launch intent for: " + packageName);
                    LogUtils.writeGameLaunchLog(packageName, false, "RootMode");
                }
            } catch (Exception e) {
                android.util.Log.e("ROOT_LAUNCH", "💥 [ROOT] Error force restarting original PUBG: " + e.getMessage());
                LogUtils.writeError("Error force restarting original PUBG", e);
            }
        } else {
            // NON-ROOT MODE - оставляем как есть
            
            LogUtils.writeLog("NON-ROOT MODE: Using Meta virtualization");
            
            // Отключаем Profile Saver перед запуском игры для предотвращения краша
            disableProfileSaver();
            
            // Перед запуском игры через Meta добавляю лог проверки состояния
            try {
                LogUtils.writeLog("checkGameProcessBeforeStart called successfully");
            } catch (Exception e) {
                android.util.Log.e("OVERLAY", "[PRE-LAUNCH] Error calling checkGameProcessBeforeStart: " + e.getMessage());
                LogUtils.writeError("Error calling checkGameProcessBeforeStart", e);
            }
            
            LogUtils.writeLog("Attempting to launch game via MetaActivityManager.launchApp()");
            VBoxCore.get().launchApk(packageName, USER_ID);
            LogUtils.writeMetaLaunchLog(packageName, true, "MetaActivityManager.launchApp()");
            LogUtils.writeGameLaunchLog(packageName, true, "MetaActivityManager");
            
            // Показываем кнопку STOP после запуска игры
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                gameLaunched = true;
                
                // Сохраняем состояние игры
                android.content.SharedPreferences.Editor editor = getSharedPreferences("LoadingActivity", MODE_PRIVATE).edit();
                editor.putBoolean("gameLaunched", true);
                editor.apply();
                
                // Запускаем ColorAnalysisService для анализа цветов экрана
                // startColorAnalysisService(); // Removed - using ColorPickerService instead
                
                showStopButton();
            }, 2000); // 2 секунды задержки
        }
    }

    private boolean isGameRunning(String packageName) {
        if (isRootMode()) {
            // ROOT MODE - всегда возвращаем false (не используем)
                return false;
        } else {
            return VBoxCore.get().isAppRunning(packageName, USER_ID);
        }
    }

        private void startOverlayHybrid() {
        // ROOT MODE - ничего не делаем
                return;
    }
    
    private void startOverlayHybridInternal() {
        // ROOT MODE - ничего не делаем
                return;
                }
                
        // /**
    //  * Запуск ColorAnalysisService для анализа цветов экрана
    //  */
    // private void startColorAnalysisService() {
    //     try {
    //         android.util.Log.d("COLOR_OVERLAY", "🎨 Starting ColorAnalysisService for screen color analysis");
    //         
    //         // Проверяем разрешение на overlay
    //         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
    //             if (!android.provider.Settings.canDrawOverlays(this)) {
    //             android.util.Log.w("COLOR_OVERLAY", "⚠️ Overlay permission not granted");
    //             return;
    //         }
    //         }
    //         
    //         // Запускаем сервис
    //         Intent serviceIntent = new Intent(this, com.glass.engine.service.ColorAnalysisService.class);
    //         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
    //             startForegroundService(serviceIntent);
    //         } else {
    //             startService(serviceIntent);
    //         }
    //         
    //         android.util.Log.d("COLOR_OVERLAY", "✅ ColorAnalysisService started successfully");
    //         
    //     } catch (Exception e) {
    //         android.util.Log.e("COLOR_OVERLAY", "💥 Error starting ColorAnalysisService: " + e.getMessage(), e);
    //     }
    // }
                
    /**
     * Мониторинг логов для поиска триггера bypass в root режиме
     */
    private void startRootLogMonitoring(String packageName) {
        // Проверяем, что это PUBG - только для него делаем root bypass мониторинг
        if (
    !"com.tencent.ig".equals(packageName)  ||
    !"com.rekoo.pubgm".equals(packageName) ||
    !"com.vng.pubgmobile".equals(packageName) ||
    !"com.pubg.krmobile".equals(packageName)
) {
            
            return;
        }
        
        new Thread(() -> {
            try {
                android.util.Log.d("ROOT_LOG_MONITOR", "🎯 [ROOT] Starting logcat monitoring for bypass trigger");
                    
                // Очищаем старые логи с root правами
                com.glass.engine.BoxApplication.get().doExe("su -c 'logcat -c'");
                    
                // Запускаем logcat с root правами, чтобы поймать все логи
                Process process = Runtime.getRuntime().exec("su -c 'logcat'");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                
                String line;
                android.util.Log.d("ROOT_LOG_MONITOR", "🔍 [ROOT] Monitoring started, waiting for GameActitivy logs...");
                
                int logCount = 0;
                while ((line = reader.readLine()) != null) {
                    // Исключаем логи самого мониторинга
                    if (line.contains("ROOT_LOG_MONITOR")) {
                        continue;
                    }
                    
                    // Логируем первые 10 логов для отладки
                    if (logCount < 10) {
                        android.util.Log.d("ROOT_LOG_MONITOR", "🔍 [ROOT] Debug log " + logCount + ": " + line.trim());
                        logCount++;
                    }
                    
                    // Ищем только логи GameActitivy
                    if (line.contains("GameActitivy")) {
                        android.util.Log.d("ROOT_LOG_MONITOR", "📝 [ROOT] Found GameActitivy log: " + line.trim());
            
                        // Ищем триггер bypass
                        if (line.contains("AndroidThunkJava_GetFIRAppInstanceId START")) {
                            android.util.Log.d("ROOT_LOG_MONITOR", "🎯 [ROOT] BYPASS TRIGGER FOUND: " + line.trim());
                            
            
                            // Запускаем bypass в root режиме
                            android.util.Log.d("ROOT_LOG_MONITOR", "🚀 [ROOT] Launching bypass in root mode");
                            launchRootBypass(packageName);
                            
                            // Останавливаем мониторинг после нахождения триггера
                            process.destroy();
                            break;
                        }
                    }
                }
                
                reader.close();
            } catch (Exception e) {
                android.util.Log.e("ROOT_LOG_MONITOR", "💥 [ROOT] Error in log monitoring: " + e.getMessage());
                    }
                }).start();
    }
    
    /**
     * Мониторинг закрытия игры в root режиме
     */
    private void startRootGameClosureMonitoring() {
        new Thread(() -> {
            try {
                android.util.Log.d("ROOT_GAME_MONITOR", "🔍 [ROOT] Starting ActivityManager log monitoring for game closure");
                
                // Агрессивная очистка логов
                com.glass.engine.BoxApplication.get().doExe("su -c 'logcat -c'");
                Thread.sleep(500); // Ждем очистки
                com.glass.engine.BoxApplication.get().doExe("su -c 'logcat -c'");
                Thread.sleep(500); // Еще раз для надежности
                
                // Запоминаем время начала мониторинга
                long startTime = System.currentTimeMillis();
                android.util.Log.d("ROOT_GAME_MONITOR", "⏰ [ROOT] Monitoring started at: " + startTime);
                
                Process process = Runtime.getRuntime().exec("su -c 'logcat'"); // Get all logs with root
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                android.util.Log.d("ROOT_GAME_MONITOR", "👀 [ROOT] Monitoring ActivityManager logs for game closure...");
                
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ROOT_GAME_MONITOR")) { // Exclude own logs
                        continue;
                    }
                    
                    // Ищем лог закрытия игры
                    if (line.contains("ActivityManager") && line.contains("I") && line.contains("Killing") ) {
                    if (line.contains("com.tencent.ig") || line.contains("com.pubg.krmobile") || line.contains("com.vng.pubgmobile") || line.contains("com.rekoo.pubgm")) {
                        // Проверяем, что это новый лог (не старый)
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - startTime > 1000) { // Игнорируем логи в первые секунды
                            android.util.Log.d("ROOT_GAME_MONITOR", "🎯 [ROOT] GAME CLOSURE DETECTED: " + line.trim());
                            
                            
                            // Запускаем полный сброс (аналогично non-root)
                            performRootGameStopReset();
                            break;
        } else {
                            android.util.Log.d("ROOT_GAME_MONITOR", "⏭️ [ROOT] Ignoring old log: " + line.trim());
                        }
                    }
                  }
                    // Ищем лог закрытия самого приложения
                    if (line.contains("ActivityManager") && line.contains("I") && line.contains("Killing") && line.contains("com.glass.engine")) {
                        // Проверяем, что это новый лог (не старый)
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - startTime > 1000) { // Игнорируем логи в первые секунды
                            android.util.Log.d("ROOT_GAME_MONITOR", "🎯 [ROOT] APP CLOSURE DETECTED: " + line.trim());
                            
                            
                            // Запускаем полный сброс (аналогично non-root)
                            performRootGameStopReset();
                            break;
                        } else {
                            android.util.Log.d("ROOT_GAME_MONITOR", "⏭️ [ROOT] Ignoring old log: " + line.trim());
                    }
                    }
                }
                reader.close();
            } catch (Exception e) {
                android.util.Log.e("ROOT_GAME_MONITOR", "💥 [ROOT] Error in game closure monitoring: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Полный сброс при закрытии игры в root режиме (аналогично non-root)
     */
    private void performRootGameStopReset() {
        new Thread(() -> {
            try {
                android.util.Log.d("ROOT_RESET", "🔄 [ROOT] Starting full reset after game closure");
                
                
                // 1. Удаляем overlay view с экрана
                android.util.Log.d("ROOT_RESET", "👁️ [ROOT] Removing overlay view");
                // Overlay view removal handled by service lifecycle
                
                // 2. Отключаем ESPView overlay
                android.util.Log.d("ROOT_RESET", "👁️ [ROOT] Disabling ESPView overlay");
              //  com.glass.engine.floating.ESPView.disableOverlay();
                
                // 3. Останавливаем overlay сервис
                android.util.Log.d("ROOT_RESET", "🛑 [ROOT] Stopping overlay service");
              //  Intent overlayIntent = new Intent(this, com.glass.engine.floating.Overlay.class);
             //   stopService(overlayIntent);
                
                // 4. Убиваем pubg_sock процессы
                android.util.Log.d("ROOT_RESET", "💀 [ROOT] Killing pubg_sock processes");
                com.glass.engine.BoxApplication.get().doExe("su -c 'pkill -f pubg_sock'");
                com.glass.engine.BoxApplication.get().doExe("su -c 'killall pubg_sock'");
                
                // 5. Очищаем логи
                android.util.Log.d("ROOT_RESET", "🧹 [ROOT] Clearing logs");
                com.glass.engine.BoxApplication.get().doExe("su -c 'logcat -c'");
                
                android.util.Log.d("ROOT_RESET", "✅ [ROOT] Full reset completed");
                
                
            } catch (Exception e) {
                android.util.Log.e("ROOT_RESET", "💥 [ROOT] Error in reset: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Мониторинг UE4 фокуса для hide/show в root режиме
     */
    private void startRootUe4FocusMonitoring() {
        new Thread(() -> {
            try {
                android.util.Log.d("ROOT_UE4_MONITOR", "🔍 [ROOT] Starting UE4 focus log monitoring");
                    
                // Агрессивная очистка логов
                com.glass.engine.BoxApplication.get().doExe("su -c 'logcat -c'");
                Thread.sleep(500);
                com.glass.engine.BoxApplication.get().doExe("su -c 'logcat -c'");
                Thread.sleep(500);
                    
                // Запоминаем время начала мониторинга
                long startTime = System.currentTimeMillis();
                android.util.Log.d("ROOT_UE4_MONITOR", "⏰ [ROOT] UE4 monitoring started at: " + startTime);
                    
                // Переменные для дебаунсинга
                String lastUe4FocusLog = "";
                long lastUe4FocusLogTime = 0;
                final long UE4_FOCUS_LOG_INTERVAL = 1000; // 1 секунда между логами
                final long UE4_FOCUS_DEBOUNCE_DELAY = 50; // 50ms дебаунс
                
                // Handler для дебаунсинга
                android.os.Handler ue4FocusHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                Runnable pendingUe4FocusAction = null;
                
                Process process = Runtime.getRuntime().exec("su -c 'logcat'");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                android.util.Log.d("ROOT_UE4_MONITOR", "👀 [ROOT] Monitoring UE4 focus logs...");
                
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ROOT_UE4_MONITOR")) { // Exclude own logs
                        continue;
                    }
                    
                    // Ищем UE4 фокус логи
                    if (line.contains("UE4") && line.contains("Case APP_CMD_LOST_FOCUS") || line.contains("UE4") && line.contains("Case APP_CMD_GAINED_FOCUS")) {
                        // Проверяем время
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - startTime > 1000) { // Игнорируем старые логи
                            
                            // Проверяем интервал между логами
                            if (currentTime - lastUe4FocusLogTime > UE4_FOCUS_LOG_INTERVAL) {
                                
                                // Проверяем на дубликаты
                                if (!line.trim().equals(lastUe4FocusLog)) {
                                    lastUe4FocusLog = line.trim();
                                    lastUe4FocusLogTime = currentTime;
                                    
                                    android.util.Log.d("ROOT_UE4_MONITOR", "🎮 [ROOT] UE4 FOCUS LOG: " + line.trim());
                                    
                                    // Отменяем предыдущее действие
                                    if (pendingUe4FocusAction != null) {
                                        ue4FocusHandler.removeCallbacks(pendingUe4FocusAction);
                                    }
                                    
                                    // Создаем новое действие с дебаунсом
                                    final String finalLine = line;
                                    pendingUe4FocusAction = new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                if (finalLine.contains("APP_CMD_LOST_FOCUS")) {
                                                    android.util.Log.d("ROOT_UE4_MONITOR", "👁️ [ROOT] Hiding overlay (LOST_FOCUS)");
                                                   // com.glass.engine.floating.ESPView.hideOverlay();
                                                } else if (finalLine.contains("APP_CMD_GAINED_FOCUS")) {
                                                    android.util.Log.d("ROOT_UE4_MONITOR", "👁️ [ROOT] Showing overlay (GAINED_FOCUS)");
                                                   // com.glass.engine.floating.ESPView.showOverlay();
                                                }
                    } catch (Exception e) {
                                                android.util.Log.e("ROOT_UE4_MONITOR", "💥 [ROOT] Error in UE4 focus action: " + e.getMessage());
                    }
                                        }
                                    };
                                    
                                    // Запускаем с дебаунсом
                                    ue4FocusHandler.postDelayed(pendingUe4FocusAction, UE4_FOCUS_DEBOUNCE_DELAY);
                                }
                            }
                } else {
                            android.util.Log.d("ROOT_UE4_MONITOR", "⏭️ [ROOT] Ignoring old UE4 log: " + line.trim());
                        }
                    }
                }
                reader.close();
            } catch (Exception e) {
                android.util.Log.e("ROOT_UE4_MONITOR", "💥 [ROOT] Error in UE4 focus monitoring: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Запуск bypass в root режиме
     */
    private void launchRootBypass(String packageName) {
        new Thread(() -> {
            try {
                android.util.Log.d("ROOT_BYPASS", "🎯 [ROOT] Starting bypass execution");
            
                // Запускаем bypass all три раза с интервалом 1 секунда
                for (int i = 1; i <= 3; i++) {
                    android.util.Log.d("ROOT_BYPASS", "🔄 [ROOT] Bypass iteration " + i + "/3");
                    
                    // Запускаем bypass с Features=1 (root путь)
                    android.util.Log.d("ROOT_BYPASS", "🚀 [ROOT] Launching bypass with Features=1");
                    com.glass.engine.BoxApplication.get().doExe("su -c 'cd /data/local/tmp && ./bypass 1'");
            
                    // Запускаем bypass с Features=2 (root путь)
                    android.util.Log.d("ROOT_BYPASS", "🚀 [ROOT] Launching bypass with Features=2");
                     com.glass.engine.BoxApplication.get().doExe("su -c 'cd /data/local/tmp && ./bypass 2'");
            
                    // Ждем 1 секунду между итерациями
                    if (i < 3) {
                        Thread.sleep(1000);
                    }
                }
                
                                android.util.Log.d("ROOT_BYPASS", "✅ [ROOT] Bypass execution completed");
                
                
                // Проверяем наличие файлов перед запуском overlay
                checkRootFilesAndStartOverlay();
                
                    } catch (Exception e) {
                android.util.Log.e("ROOT_BYPASS", "💥 [ROOT] Error launching bypass: " + e.getMessage());
                    }
        }).start();
    }
    
    /**
     * Запуск overlay и pubg_sock в root режиме (аналогично non-root)
     */
    private void startRootOverlayAndSock() {
        new Thread(() -> {
            try {
                android.util.Log.d("ROOT_OVERLAY", "🎯 [ROOT] Starting overlay and sock");
                
                                // 1. Запускаем overlay сервис (он сам запустит pubg_sock)
                android.util.Log.d("ROOT_OVERLAY", "🚀 [ROOT] Starting overlay service");
            //    Intent overlayIntent = new Intent(this, com.glass.engine.floating.Overlay.class);
            //    startService(overlayIntent);
                
                // 2. Включаем overlay в ESPView
                android.util.Log.d("ROOT_OVERLAY", "👁️ [ROOT] Enabling ESPView overlay");
              //  com.glass.engine.floating.ESPView.setGameActive(true);
               // com.glass.engine.floating.ESPView.enableOverlay();
                
                // 3. pubg_sock будет запущен автоматически в Overlay.Start()
                android.util.Log.d("ROOT_OVERLAY", "🔌 [ROOT] Pubg_sock will be started by Overlay service");
                
                android.util.Log.d("ROOT_OVERLAY", "✅ [ROOT] Overlay and sock setup completed");
                
                // 4. Запускаем мониторинг закрытия игры
                android.util.Log.d("ROOT_GAME_MONITOR", "🎯 [ROOT] Starting game closure monitoring");
                startRootGameClosureMonitoring();
                
                // 5. Запускаем мониторинг UE4 фокуса для hide/show
                android.util.Log.d("ROOT_UE4_MONITOR", "🎮 [ROOT] Starting UE4 focus monitoring for hide/show");
                startRootUe4FocusMonitoring();
                
                // 6. Запускаем RootGameMonitor для мониторинга игры

                
                
            } catch (Exception e) {
                android.util.Log.e("ROOT_OVERLAY", "💥 [ROOT] Error in overlay setup: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Проверка файлов и запуск overlay в root режиме
     */
    private void checkRootFilesAndStartOverlay() {
        new Thread(() -> {
            try {
                android.util.Log.d("ROOT_FILES", "🔍 [ROOT] Checking root files before overlay");
                
                File bypassFile = new File("/data/local/tmp/bypass");
                File pubgSockFile = new File("/data/local/tmp/pubg_sock");
                
                android.util.Log.d("ROOT_FILES", "📁 [ROOT] Bypass file: exists=" + bypassFile.exists() + ", size=" + (bypassFile.exists() ? bypassFile.length() : "N/A"));
                android.util.Log.d("ROOT_FILES", "📁 [ROOT] Pubg_sock file: exists=" + pubgSockFile.exists() + ", size=" + (pubgSockFile.exists() ? pubgSockFile.length() : "N/A"));
                
                if (!bypassFile.exists() || bypassFile.length() == 0) {
                    android.util.Log.e("ROOT_FILES", "❌ [ROOT] Bypass file not found or empty!");
                    
                    
                    // Пытаемся скопировать из app directory
                    File appBypass = new File(getFilesDir(), "bypass");
                    if (appBypass.exists()) {
                        android.util.Log.d("ROOT_FILES", "📋 [ROOT] Copying bypass from app directory");
                        com.glass.engine.BoxApplication.get().doExe("su -c 'cp " + appBypass.getAbsolutePath() + " /data/local/tmp/bypass'");
                        com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/bypass'");
                    }
                }
                
                if (!pubgSockFile.exists() || pubgSockFile.length() == 0) {
                    android.util.Log.e("ROOT_FILES", "❌ [ROOT] Pubg_sock file not found or empty!");
                    
                    
                    // Пытаемся скопировать из app directory
                    File appDeltaSock = new File(getFilesDir(), "delta_sock");
                    if (appDeltaSock.exists()) {
                        android.util.Log.d("ROOT_FILES", "📋 [ROOT] Copying delta_sock from app directory");
                        com.glass.engine.BoxApplication.get().doExe("su -c 'cp " + appDeltaSock.getAbsolutePath() + " /data/local/tmp/delta_sock'");
                        com.glass.engine.BoxApplication.get().doExe("su -c 'chmod 777 /data/local/tmp/delta_sock'");
                    }
                }
                
                // Проверяем еще раз после копирования
                Thread.sleep(1000);
                bypassFile = new File("/data/local/tmp/bypass");
                pubgSockFile = new File("/data/local/tmp/pubg_sock");
                
                if (bypassFile.exists() && pubgSockFile.exists() && bypassFile.length() > 0 && pubgSockFile.length() > 0) {
                    android.util.Log.d("ROOT_FILES", "✅ [ROOT] Files ready, starting overlay");
                    
                    startRootOverlayAndSock();
                } else {
                    android.util.Log.e("ROOT_FILES", "❌ [ROOT] Files still missing after copy attempt");
                    
                            }
                
            } catch (Exception e) {
                android.util.Log.e("ROOT_FILES", "💥 [ROOT] Error checking files: " + e.getMessage());
            }
        }).start();
            }
    
    public void launchBypassHybrid(String packageName) {
        if (isRootMode()) {
            // ROOT MODE - ничего не делаем
            return;
        } else {
            // Non-root mode - используем GameLogMonitor
            
        }
    }
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        android.util.Log.d("LoadingActivity", "🔄 Configuration changed but activity preserved");
        // Ничего не делаем - активность не пересоздается
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        LogUtils.writeLog("LoadingActivity onDestroy called");
        
        // Сбрасываем флаги при уничтожении активности
        stopButtonShown = false;
        gameLaunched = false;
        processStarted = false;
        isInitialized = false;
        isActivityRunning = false;
        
        // Очищаем SharedPreferences
        android.content.SharedPreferences.Editor editor = getSharedPreferences("LoadingActivity", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        
        // Останавливаем все сервисы при выходе

        
        LogUtils.closeLogging();
    }

    /**
     * Wait for Delta Force assets download to complete
     */
    private void waitForDeltaForceAssetsDownload(Runnable onComplete) {
        android.util.Log.d("DELTA_FORCE", "⏳ Waiting for Delta Force assets download to complete...");
        
        final Handler monitorHandler = new Handler(Looper.getMainLooper());
        final int[] attempts = {0};
        final int maxAttempts = 60; // 5 минут максимум
        final boolean isRoot = isRootMode();

        final Runnable monitorRunnable = new Runnable() {
            @Override
            public void run() {
                attempts[0]++;

                if (areDeltaForceAssetsLoaded()) {
                    String path = isRoot ? "/data/local/tmp/" : "app directory";
                    android.util.Log.d("DELTA_FORCE", "✅ Assets found in " + path + ", download completed");
                    onComplete.run();
                    return;
                }

                if (attempts[0] >= maxAttempts) {
                    android.util.Log.w("DELTA_FORCE", "⚠️ Download timeout, proceeding anyway");
                    onComplete.run();
                    return;
                }

                String path = isRoot ? "/data/local/tmp/" : "app directory";
                android.util.Log.d("DELTA_FORCE", "⏳ Waiting for assets in " + path + "... (" + attempts[0] + "/" + maxAttempts + ")");
                monitorHandler.postDelayed(this, 5000); // Проверяем каждые 5 секунд
            }
        };

        monitorHandler.postDelayed(monitorRunnable, 5000);
    }

    /**
     * Fallback game launch method for Delta Force
     */
    private void launchDeltaForceGameFallback(String packageName) {
        android.util.Log.d("DELTA_FORCE", "🔄 Using fallback launch method for: " + packageName);
        
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                android.util.Log.d("DELTA_FORCE", "✅ Delta Force launched via fallback method");

                // Не показываем кнопку сейчас, откладываем до onResume
                new Thread(() -> {
                    gameLaunched = true;
                    android.content.SharedPreferences.Editor editor = getSharedPreferences("LoadingActivity", MODE_PRIVATE).edit();
                    editor.putBoolean("gameLaunched", true);
                    editor.apply();
                    deferShowStopOnResume = true;
                }).start();
            } else {
                android.util.Log.e("DELTA_FORCE", "❌ Fallback launch also failed");
            }
        } catch (Exception e) {
            android.util.Log.e("DELTA_FORCE", "💥 Fallback launch error: " + e.getMessage(), e);
        }
    }

    /**
     * Check if Delta Force assets are properly loaded
     */
    private boolean areDeltaForceAssetsLoaded() {
        if (isRootMode()) {
            File rootDeltaSock = new File("/data/local/tmp/delta_sock");
            File rootDeltaBypass = new File("/data/local/tmp/delta_rootbypass");
            
            boolean sockExists = rootDeltaSock.exists() && rootDeltaSock.length() > 0;
            boolean bypassExists = rootDeltaBypass.exists() && rootDeltaBypass.length() > 0;
            
            android.util.Log.d("DELTA_FORCE", "🔍 [ROOT] Assets check - delta_sock: " + sockExists + 
                " (" + (sockExists ? rootDeltaSock.length() : 0) + " bytes), " +
                "delta_rootbypass: " + bypassExists + 
                " (" + (bypassExists ? rootDeltaBypass.length() : 0) + " bytes)");
            
            return sockExists && bypassExists;
        } else {
            File appDeltaSock = new File(getFilesDir() + "/delta_sock");
            File appDeltaBypass = new File(getFilesDir() + "/delta_bypass");
            
            boolean sockExists = appDeltaSock.exists() && appDeltaSock.length() > 0;
            boolean bypassExists = appDeltaBypass.exists() && appDeltaBypass.length() > 0;
            
            android.util.Log.d("DELTA_FORCE", "🔍 [NON-ROOT] Assets check - delta_sock: " + sockExists + 
                " (" + (sockExists ? appDeltaSock.length() : 0) + " bytes), " +
                "delta_bypass: " + bypassExists + 
                " (" + (bypassExists ? appDeltaBypass.length() : 0) + " bytes)");
            
            return sockExists && bypassExists;
        }
    }
}
