package com.glass.engine.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glass.engine.R;
import com.glass.engine.BoxApplication;

import com.glass.engine.utils.ActivityCompat;
import com.glass.engine.utils.DeviceUtils;

import androidx.core.view.ViewCompat;
import android.app.AlertDialog;

import eightbitlab.com.blurview.BlurView;
import com.vbox.VBoxCore;


public class MainActivity extends ActivityCompat {
     static {
        try {
            System.loadLibrary("client");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    public static native boolean ich();
    // Флаг для предотвращения двойного срабатывания onResume
    private boolean onResumeExecuted = false;

    private long backPressedTime = 0;
    static MainActivity instance;
    private static final String PREFS_NAME = "orientation_prefs";
    private static final String KEY_LANDSCAPE_LOCK = "landscape_lock";

    
    
    // Нативные методы для загрузки настроек ESP
    

    public static native String exdate();

public static native boolean getesp();
    public static MainActivity get() {
        return instance;
    }

    private static boolean isLandscapeLockActive(Context ctx) {
        try {
            return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_LANDSCAPE_LOCK, false);
        } catch (Exception e) { return false; }
    }

    public static void setLandscapeLockActive(Context ctx, boolean active) {
        try {
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LANDSCAPE_LOCK, active)
                .apply();
        } catch (Exception e) { }
    }

    public static void goMain(Context context) {
        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        
        // Для планшетов разрешаем поворот экрана
        if (DeviceUtils.isTablet(this)) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

 @SuppressLint({"WrongViewCast", "NonConstantResourceId", "ClickableViewAccessibility", "WrongConstant"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean isTablet = DeviceUtils.isTablet(this);
        android.util.Log.d("DeviceUtils", "[MainActivity] isTablet=" + isTablet);
         boolean ich = ich();
        // ВСЕГДА начинаем в портретной ориентации и сбрасываем флаг ландшафтной блокировки
    //    setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    //    setLandscapeLockActive(this, false); // Сбрасываем флаг при создании приложения
        android.util.Log.d("MainActivity", "🔄 onCreate - ALWAYS start in PORTRAIT and LOCK orientation, reset landscape flag");

        super.onCreate(savedInstanceState);
        
        // Запрашиваем разрешения для поворота экрана и overlay
     //   requestScreenPermissions();
        
        // Принудительно устанавливаем максимальную частоту обновления
        forceHighRefreshRate();
        
        setContentView(R.layout.activity_main);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

        // Полноэкранный режим наследуется из ActivityCompat
        isLogin = true;
        instance = this;

        // Проверяем и показываем статус root для гибридного режима
        checkAndShowRootStatus();



        // Hide contenttt before activation
        View contentView = findViewById(R.id.contenttt);
        if (contentView != null) {
            contentView.setVisibility(View.INVISIBLE);
        }

        // Handle bottom bar insets
        View bottomBar = findViewById(R.id.bottom_bar);
        if (bottomBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, insets) -> {
                int bottomInset = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bottomInset = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
                }
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                params.bottomMargin = bottomInset + 10;
                v.setLayoutParams(params);
                return insets;
            });
        }

        // --- Bottom bar BlurView ОТКЛЮЧЕН для повышения FPS ---
        BlurView bottomBarBlur = findViewById(R.id.bottom_bar_blur);
        if (bottomBarBlur != null) {
            bottomBarBlur.setVisibility(View.GONE);
            android.util.Log.d("MainActivity", "🚫 Bottom bar blur disabled for performance");
        }
        // Set default fragment on app start (MainContentFragment)
        try {
            androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
            if (!fragmentManager.isDestroyed() && !fragmentManager.isStateSaved()) {
                fragmentManager.beginTransaction()
                    .replace(R.id.contenttt, new com.glass.engine.fragments.MainContentFragment())
                    .commitAllowingStateLoss();
            }
        } catch (Exception e) {
        }
        
        // Загружаем сохраненные настройки ESP при запуске MainActivity
     //   loadEspSettings();

        // --- Bottom navigation setup ---
        ImageView navHome = findViewById(R.id.nav_main);
        ImageView navLogin = findViewById(R.id.nav_login);
        ImageView navSettings = findViewById(R.id.nav_apps);
        ImageView navProfile = findViewById(R.id.nav_profile);

        View.OnClickListener navClickListener = v -> {
            navHome.setSelected(false);
             if (!ich){
            navLogin.setSelected(false);
            }
            navSettings.setSelected(false);
            navProfile.setSelected(false);
            v.setSelected(true);

            androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.contenttt);

            if (v.getId() == R.id.nav_main) {
                if (!(currentFragment instanceof com.glass.engine.fragments.MainContentFragment)) {
                    View contentView2 = findViewById(R.id.contenttt);
                    if (contentView2 != null) {
                        contentView2.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                try {
                                    androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
                                    if (!fragmentManager.isDestroyed() && !fragmentManager.isStateSaved()) {
                                        fragmentManager.beginTransaction()
                                            .replace(R.id.contenttt, new com.glass.engine.fragments.MainContentFragment())
                                            .commitAllowingStateLoss();
                                    }
                                } catch (Exception e) {
                                }
                                contentView2.setAlpha(0f);
                                contentView2.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start();
                            }).start();
                    }
                }
            } else if (!ich && v.getId() == R.id.nav_login) {
                if (!(currentFragment instanceof com.glass.engine.fragments.LoginFragment)) {
                    View contentView2 = findViewById(R.id.contenttt);
                    if (contentView2 != null) {
                        contentView2.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                try {
                                    androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
                                    if (!fragmentManager.isDestroyed() && !fragmentManager.isStateSaved()) {
                                        fragmentManager.beginTransaction()
                                            .replace(R.id.contenttt, new com.glass.engine.fragments.LoginFragment())
                                            .commitAllowingStateLoss();
                                    }
                                } catch (Exception e) {
                                }
                                contentView2.setAlpha(0f);
                                contentView2.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start();
                            }).start();
                    }
                }
            } else if (v.getId() == R.id.nav_apps) {
                if (!(currentFragment instanceof com.glass.engine.fragments.LoginAppsFragment)) {
                    View contentViewNav = findViewById(R.id.contenttt);
                    if (contentViewNav != null) {
                        contentViewNav.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                try {
                                    androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
                                    if (!fragmentManager.isDestroyed() && !fragmentManager.isStateSaved()) {
                                        fragmentManager.beginTransaction()
                                            .replace(R.id.contenttt, new com.glass.engine.fragments.LoginAppsFragment())
                                            .commitAllowingStateLoss();
                                    }
                                } catch (Exception e) {
                                }
                                contentViewNav.setAlpha(0f);
                                contentViewNav.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start();
                            }).start();
                    }
                }
            } else if (v.getId() == R.id.nav_profile) {
                if (!(currentFragment instanceof com.glass.engine.fragments.SellersFragment)) {
                    View contentViewProfile = findViewById(R.id.contenttt);
                    if (contentViewProfile != null) {
                        contentViewProfile.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction(() -> {
                                try {
                                    androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
                                    if (!fragmentManager.isDestroyed() && !fragmentManager.isStateSaved()) {
                                        fragmentManager.beginTransaction()
                                            .replace(R.id.contenttt, new com.glass.engine.fragments.SellersFragment())
                                            .commitAllowingStateLoss();
                                    }
                                } catch (Exception e) {
                                }
                                contentViewProfile.setAlpha(0f);
                                contentViewProfile.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start();
                            }).start();
                    }
                }
            }
        };

        if (navHome != null) navHome.setOnClickListener(navClickListener);
        if (!ich){
        if (navLogin != null) navLogin.setOnClickListener(navClickListener);
        }
        if (navSettings != null) navSettings.setOnClickListener(navClickListener);
        if (navProfile != null) navProfile.setOnClickListener(navClickListener);

        // Default selected
        if (ich){
        if (navHome != null) navHome.setSelected(true);
        }else{
        if (navLogin != null) navLogin.setSelected(true);
        }

        FrameLayout overlay = findViewById(R.id.centered_nav_overlay);
        View navContainer = findViewById(R.id.centered_nav_container);
        ImageView menuButton = findViewById(R.id.menu_button);
        startBackgroundAnimation();
        if (menuButton != null && overlay != null && navContainer != null) {
            menuButton.setOnClickListener(v -> {
                overlay.setVisibility(View.VISIBLE);
                applyFadeIn(navContainer);
            });

            overlay.setOnClickListener(v -> overlay.setVisibility(View.GONE));
        }
        // Telegram contact click listener
        TextView telegramContact = findViewById(R.id.contact_telegram);
        if (telegramContact != null) {
            telegramContact.setOnClickListener(v -> {
                String url = LoginActivity.GetKey();
                Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
                startActivity(intent);
            });
        }


        ImageView logoutButton = findViewById(R.id.logo_logout);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }





        new Handler(Looper.getMainLooper()).post(() -> {
            View root = getWindow().getDecorView().getRootView();
            if (root != null) {
                root.requestFocus();
            }
        });



        // BlurViewOverlay ОТКЛЮЧЕН для повышения FPS
        BlurView blurViewOverlay = findViewById(R.id.blurViewBack);
        if (blurViewOverlay != null) {
            blurViewOverlay.setVisibility(View.GONE);
            android.util.Log.d("MainActivity", "🚫 BlurViewOverlay disabled for performance");
        }


        // BlurView blurww ОТКЛЮЧЕН для повышения FPS
        BlurView blurww = findViewById(R.id.blurww);
        if (blurww != null) {
            blurww.setVisibility(View.GONE);
            android.util.Log.d("MainActivity", "🚫 BlurView blurww disabled for performance");
        }


        // Полноэкранный режим наследуется из ActivityCompat



        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // В root режиме пропускаем проверку активации Meta
                boolean isRootMode = false;
                boolean isActivated = true;
                
                if (!isActivated) {





                  new Handler(Looper.getMainLooper()).postDelayed(this, 1000);






                } else {
                    View contentViewDelayed = findViewById(R.id.contenttt);
                    if (contentViewDelayed != null) {
                        contentViewDelayed.setVisibility(View.VISIBLE);
                        applyFadeIn(contentViewDelayed);
                    }
                    showMainUI();
                    // Launch any post-animation initialization
                    runOnUiThread(() -> {
                        // Проверяем, что активность не уничтожена
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (ich){
                        if (navHome != null) navHome.performClick();
                        showMainUI();
                        }else{
                        if (navLogin != null) navLogin.performClick();
                        
                        showMainUI();
                        }
                        // Проверяем состояние FragmentManager перед добавлением фрагмента
                        try {
                            androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
                            if (!fragmentManager.isDestroyed() && !fragmentManager.isStateSaved()) {
                                androidx.fragment.app.Fragment currentFragment = fragmentManager.findFragmentById(R.id.contenttt);
                                if (currentFragment == null) {
                                    fragmentManager.beginTransaction()
                                        .setCustomAnimations(
                                            R.anim.fade_in, // enter (анимация при первом появлении)
                                            0,              // exit
                                            R.anim.fade_in, // popEnter (анимация при возврате назад)
                                            0               // popExit
                                        )
                                        .replace(R.id.contenttt, new com.glass.engine.fragments.MainContentFragment())
                                        .commitAllowingStateLoss();
                                }
                            }
                        } catch (Exception e) {
                        }
                    });
                    // UI fade-ins, filter buttons, and filter selection now handled in MainContentFragment
                }
            }
        }, 1000);


    }

    private void startBackgroundAnimation() {
        ImageView back = findViewById(R.id.back);
        ImageView back2 = findViewById(R.id.back2);

        if (back != null) {

            back.animate()
                    .rotationBy(360)
                    .setDuration(90000)
                    .setInterpolator(new android.view.animation.LinearInterpolator())
                    .withEndAction(this::startBackgroundAnimation)
                    .start();


            float offset = (float) (Math.random() * 50 - 50);
            back.animate()
                    .translationYBy(offset)
                    .setDuration(5000)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .withEndAction(() -> startBackgroundAnimation())
                    .start();
        }

        if (back2 != null) {

            back2.animate()
                    .rotationBy(-360)
                    .setDuration(90000)
                    .setInterpolator(new android.view.animation.LinearInterpolator())
                    .withEndAction(this::startBackgroundAnimation)
                    .start();


            float offset2 = (float) (Math.random() * 50 - 50);
            back2.animate()
                    .translationYBy(offset2)
                    .setDuration(5000)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .withEndAction(() -> startBackgroundAnimation())
                    .start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Предотвращаем двойное срабатывание
        if (onResumeExecuted) {
            android.util.Log.d("MainActivity", "🔄 onResume - already executed, skipping");
            return;
        }
        
        // При возвращении в приложение НЕ меняем ориентацию вообще
        // Оставляем как есть (landscape если был, портрет если был)
        android.util.Log.d("MainActivity", "🔄 onResume - NO orientation changes, keeping current");
        
        // Отмечаем что onResume уже выполнен
        onResumeExecuted = true;
        

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Сбрасываем флаг при уходе из приложения
        onResumeExecuted = false;
        android.util.Log.d("MainActivity", "🔄 onPause - reset onResume flag");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Сбрасываем флаг ландшафтной блокировки при закрытии приложения
        setLandscapeLockActive(this, false);
        android.util.Log.d("MainActivity", "🔄 onDestroy - reset landscape lock flag");
        
        // Очищаем статические ссылки
        instance = null;
        
        // Полная очистка при закрытии MainActivity
        cleanupOnAppExit();
        

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        android.util.Log.d("ESP_ORIENTATION", "MainActivity onConfigurationChanged - orientation: " + 
            newConfig.orientation + " (1=portrait, 2=landscape) - NO CHANGES");
        
        // ВООБЩЕ НЕ ТРОГАЕМ ОРИЕНТАЦИЮ В onConfigurationChanged
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view instanceof EditText) {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            float x = ev.getRawX() + view.getLeft() - location[0];
            float y = ev.getRawY() + view.getTop() - location[1];

            if (ev.getAction() == MotionEvent.ACTION_DOWN && (x < view.getLeft() || x >= view.getRight() || y < view.getTop() || y >= view.getBottom())) {
                view.clearFocus();
                // Removed searchBox/adapter/recyclerView block - now handled in MainContentFragment
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }


    @Override
    public void onBackPressed() {
        androidx.fragment.app.Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.contenttt);
        if (currentFragment instanceof com.glass.engine.fragments.GameInfoFragment) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - backPressedTime > 2000) {
                backPressedTime = currentTime;
                Toast.makeText(this, R.string.press_back_again_to_return, Toast.LENGTH_SHORT).show();
            } else {
                ((com.glass.engine.fragments.GameInfoFragment) currentFragment).returnToMainLayout();
            }
            return;
        }
        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null && mainContent.getVisibility() == View.VISIBLE) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - backPressedTime > 2000) {
                backPressedTime = currentTime;
                Toast.makeText(this, R.string.press_back_again_to_exit, Toast.LENGTH_SHORT).show();
            } else {
                // При двойном нажатии отключаем overlay перед выходом

                

                Toast.makeText(this, getString(R.string.overlay_force_disabled), Toast.LENGTH_SHORT).show();
                super.onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }






    private void applyFadeIn(View view) {
        if (view == null) return;
        view.setAlpha(0f);
        view.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
    }

    // Replace fragment in drawer_layout without any fade animation
    public void replaceFragmentWithFade(androidx.fragment.app.Fragment fragment) {
        try {
            androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
            if (!fragmentManager.isDestroyed() && !fragmentManager.isStateSaved()) {
                fragmentManager.beginTransaction()
                    .replace(R.id.drawer_layout, fragment)
                    .commitAllowingStateLoss();
            }
        } catch (Exception e) {
        }
    }


    // Show main UI elements and hide fragment container
    private void showMainUI() {
        View contentView = findViewById(R.id.contenttt);
        if (contentView != null) contentView.setVisibility(View.VISIBLE);

        View fragmentContainer = findViewById(R.id.fragment_container);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
    }

    /**
     * Проверяет и показывает статус root для гибридного режима
     */
    private void checkAndShowRootStatus() {
        try {
            boolean hasRoot = BoxApplication.get().checkRootAccess();
            String rootStatus = hasRoot ? "ROOT MODE" : "NON-ROOT MODE";
            String rootColor = hasRoot ? "#00FF00" : "#FF8800";
            
            // Логируем статус
            
            // Показываем toast с статусом
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Принудительно отключает overlay
     */




    /**
     * Тестирует восстановление overlay (для отладки)
     */

    /**
     * Тестирует Usage Access функциональность
     */

    
    private void cleanupOnAppExit() {
        try {
            
            // Останавливаем overlay сервис
            try {
                // com.glass.engine.floating.Overlay.forceStopOverlay(); // Удалено
            } catch (Exception e) {
            }
            
            // Убиваем sock cheat
            try {
                killSockCheatProcess();
            } catch (Exception e) {
            }
            
            // Сбрасываем все флаги в GameLogMonitor

            
            // Останавливаем мониторинг логов

            
            // Сбрасываем флаги в ESPView
            try {
              //  com.glass.engine.floating.ESPView.setGameActive(false);
              //  com.glass.engine.floating.ESPView.disableOverlay();
            } catch (Exception e) {
            }
            
            
        } catch (Exception e) {
        }
    }
    
    private void killSockCheatProcess() {
        try {
            
            // Убиваем процесс pubg_sock
            Process killProcess = Runtime.getRuntime().exec("pkill -f pubg_sock");
            killProcess.waitFor();
            
            // Также убиваем через killall
            Process killallProcess = Runtime.getRuntime().exec("killall pubg_sock");
            killallProcess.waitFor();
            
        } catch (Exception e) {
        }
    }
    
    /**
     * Загружает сохраненные настройки ESP из SharedPreferences
     */
     



    /**
     * Функция для поворота экрана и создания черного экрана перед запуском игры
     */
    public static void prepareScreenForGame(android.content.Context context) {
        try {
            android.util.Log.d("MainActivity", "🎮 Preparing screen for game launch...");
            
            // ШАГ 1: Разблокируем поворот экрана и поворачиваем в landscape
            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                
                // Сначала разблокируем поворот экрана
                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                android.util.Log.d("MainActivity", "✅ Step 1: Screen rotation unlocked");
                
                // Через 100мс поворачиваем в landscape
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        android.util.Log.d("MainActivity", "✅ Step 1: Screen rotated to landscape");
                        
                        // Через еще 200мс ПОЛНОСТЬЮ БЛОКИРУЕМ в landscape ориентации
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                                android.util.Log.d("MainActivity", "✅ Step 1: Screen FULLY LOCKED in landscape orientation");
                                setLandscapeLockActive(activity, true);
                                
                                // Таймер для возврата в портретную ориентацию теперь запускается в GameInfoFragment после реального запуска игры
                                android.util.Log.d("MainActivity", "✅ Step 1: Screen FULLY LOCKED in landscape orientation - portrait timer will be started after game launch");
                                
                            } catch (Exception e) {
                                android.util.Log.e("MainActivity", "❌ Error locking landscape: " + e.getMessage());
                            }
                        }, 200);
                        
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "❌ Error rotating screen: " + e.getMessage());
                    }
                }, 100);
            }
            
            // ШАГ 2: Пытаемся показать черный экран (если есть разрешение)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(context)) {
                    android.util.Log.w("MainActivity", "⚠️ Step 2: Overlay permission not granted, black screen skipped");
                    return; // Выходим, экран уже повернут
                }
            }
            
            // Создаем черный экран поверх всего
            android.view.WindowManager windowManager = (android.view.WindowManager) context.getSystemService(android.content.Context.WINDOW_SERVICE);
            android.view.WindowManager.LayoutParams params = new android.view.WindowManager.LayoutParams(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT
            );
            
            android.view.View blackView = new android.view.View(context);
            blackView.setBackgroundColor(android.graphics.Color.BLACK);
            
            // Добавляем черный экран
            windowManager.addView(blackView, params);
            android.util.Log.d("MainActivity", "✅ Step 2: Black screen overlay added");
            
            // Через 1.5 секунды убираем черный экран (экран уже повернут)
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    // Убираем черный экран
                    windowManager.removeView(blackView);
                    android.util.Log.d("MainActivity", "✅ Step 2: Black screen overlay removed");
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "❌ Error removing black screen: " + e.getMessage());
                }
            }, 1500); // Уменьшил время показа черного экрана
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Error preparing screen for game: " + e.getMessage());
        }
    }

    /**
     * Возвращает экран в портретную ориентацию и блокирует её
     */
    public static void returnToPortraitOrientation(android.content.Context context) {
        try {
            if (context instanceof MainActivity) {
                MainActivity activity = (MainActivity) context;
                activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                android.util.Log.d("MainActivity", "🔄 Returning to portrait orientation and LOCKING it");
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Error returning to portrait: " + e.getMessage());
        }
    }



    /**
     * Запрашивает разрешения для поворота экрана и overlay
     */
    

    /**
     * Принудительно устанавливает максимальную частоту обновления
     */
    private void forceHighRefreshRate() {
        try {
            android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
            
            // Пробуем разные высокие частоты
            lp.preferredRefreshRate = 144f; // Максимальная
            getWindow().setAttributes(lp);
            android.util.Log.d("MainActivity", "🎯 Set preferred refresh rate to: 144Hz");
            
            // Альтернативный способ для Android R+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    android.view.Display display = getWindowManager().getDefaultDisplay();
                    android.view.Display.Mode[] modes = display.getSupportedModes();
                    android.view.Display.Mode highestMode = modes[0];
                    
                    for (android.view.Display.Mode mode : modes) {
                        if (mode.getRefreshRate() > highestMode.getRefreshRate()) {
                            highestMode = mode;
                        }
                    }
                    
                    lp.preferredDisplayModeId = highestMode.getModeId();
                    getWindow().setAttributes(lp);
                    android.util.Log.d("MainActivity", "🎯 Also set display mode to: " + highestMode.getRefreshRate() + "Hz");
                } catch (Exception e) {
                    android.util.Log.w("MainActivity", "⚠️ Display mode fallback failed: " + e.getMessage());
                }
            }
            
            // Дополнительно пытаемся через системные свойства
            try {
                Runtime.getRuntime().exec("setprop debug.sf.disable_backpressure 1");
                Runtime.getRuntime().exec("setprop debug.sf.latch_unsignaled 1");
                android.util.Log.d("MainActivity", "🔧 Applied system refresh rate tweaks");
            } catch (Exception e) {
                android.util.Log.w("MainActivity", "⚠️ Could not apply system tweaks: " + e.getMessage());
            }
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Error setting refresh rate: " + e.getMessage());
        }
    }
}