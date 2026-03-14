package com.glass.engine.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.glass.engine.BoxApplication;
import com.glass.engine.R;
import com.glass.engine.utils.ActivityCompat;
import com.glass.engine.utils.DeviceUtils;

import eightbitlab.com.blurview.BlurView;

import android.content.SharedPreferences;

public class LoginActivity extends ActivityCompat {

    static {
        try {
            System.loadLibrary("client");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    private static final String USER = "USER";
    public static String USERKEY;
    private ProgressBar progressBar;
    private long lastTouchTime = 0;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        
        // Для планшетов разрешаем поворот экрана
        if (DeviceUtils.isTablet(this)) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean isTablet = DeviceUtils.isTablet(this);
        // android.util.Log.d("DeviceUtils", "[LoginActivity] isTablet=" + isTablet);
        if (isTablet) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        
        isLogin = true;
        super.onCreate(savedInstanceState);

        if (isHttpSnifferDetected()) {
            Toast.makeText(this, getString(R.string.http_sniffer_or_proxy_detected), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Проверяем, что все разрешения предоставлены и пользователь авторизован
        try {
            // Убираем автоматический переход - всегда показываем LoginActivity
            // android.util.Log.d("LoginActivity", "Starting LoginActivity (auto-skip disabled)");
        } catch (Exception e) {
            // android.util.Log.e("LoginActivity", "Error in permission/authorization check: " + e.getMessage());
            // Продолжаем выполнение, показываем LoginActivity
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        setContentView(R.layout.activity_login);
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

        View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            rootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
                int statusBarHeight = insets.getSystemWindowInsetTop();
                v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
                return insets.consumeSystemWindowInsets();
            });
        }

        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        progressBar = findViewById(R.id.circularProgressBar);

        setupRootTouchListener();
        initView();

        // Fade-in animation for the entire screen (rootLayout)
        View rootLayoutFade = findViewById(R.id.rootLayout);
        if (rootLayoutFade != null) {
            rootLayoutFade.setAlpha(0f);
            rootLayoutFade.post(() -> rootLayoutFade.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start());
        }

        // BlurViewCard ОТКЛЮЧЕН для повышения FPS
        BlurView blurViewCard = findViewById(R.id.blurViewCard);
        if (blurViewCard != null) {
            blurViewCard.setVisibility(View.GONE);
            android.util.Log.d("LoginActivity", "🚫 BlurViewCard disabled for performance");
        }

        animateBackImages();
        
        // Запускаем мониторинг overlay и sock в LoginActivity
     //   startOverlaySockMonitor();

        // BlurViewOverlay ОТКЛЮЧЕН для повышения FPS
        BlurView blurViewOverlay = findViewById(R.id.blurViewOverlay);
        if (blurViewOverlay != null) {
            blurViewOverlay.setVisibility(View.GONE);
            android.util.Log.d("LoginActivity", "🚫 BlurViewOverlay disabled for performance");
        }


        TextView welcomeText = findViewById(R.id.welcometext);
        TextView signText = findViewById(R.id.signtext);
        TextView ortext = findViewById(R.id.or);
        TextView dontText = findViewById(R.id.dontteext);
        MaterialButton joinButton = findViewById(R.id.join);

        applyWhiteGradientToText(welcomeText);
        applyWhiteGradientToText(signText);
        applyWhiteGradientToText(ortext);
        applyWhiteGradientToText(dontText);
        applyWhiteGradientToText((TextView) joinButton);


        TextView getkey = findViewById(R.id.getKeyText);

        applyBlueGradientToText(getkey);


        FrameLayout passwordLayout = findViewById(R.id.passwordLayout);
        if (passwordLayout != null && DeviceUtils.isTablet(this)) {
            int maxWidthDp = 420;
            float density = getResources().getDisplayMetrics().density;
            int maxWidthPx = (int) (maxWidthDp * density);

            ViewGroup.LayoutParams params = passwordLayout.getLayoutParams();
            if (params != null) {
                params.width = maxWidthPx;
                passwordLayout.setLayoutParams(params);
            }


            if (passwordLayout.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) passwordLayout.getLayoutParams()).gravity = android.view.Gravity.CENTER;
            }
        }
        if (DeviceUtils.isTablet(this)) {
            ImageView back = findViewById(R.id.back);
            ImageView back2 = findViewById(R.id.back2);

            if (back != null) {
                ViewGroup.LayoutParams params = back.getLayoutParams();
                params.width *= 2;
                params.height *= 2;
                back.setLayoutParams(params);
            }

            if (back2 != null) {
                ViewGroup.LayoutParams params = back2.getLayoutParams();
                params.width *= 2;
                params.height *= 2;
                back2.setLayoutParams(params);
                back2.setTranslationX(back2.getTranslationX() * 2);
            }
            
            // Настройка mainContent для планшетов - ширина равна высоте
            View mainContent = findViewById(R.id.mainContent);
            if (mainContent != null) {
                // Получаем размеры экрана
                android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                
                // Вычисляем размер (берем меньшее из ширины или высоты экрана)
                int screenSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
                
                // Устанавливаем квадратную форму для mainContent
                ViewGroup.LayoutParams mainContentParams = mainContent.getLayoutParams();
                mainContentParams.width = screenSize;
                mainContentParams.height = screenSize;
                mainContent.setLayoutParams(mainContentParams);
                
                // Центрируем mainContent
                if (mainContent.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    ((FrameLayout.LayoutParams) mainContent.getLayoutParams()).gravity = android.view.Gravity.CENTER;
                }
            }
        }
        animatePasswordLayout();

        // --- Анимация пульсации для синего круга ---
        // (блок с ObjectAnimator для bgCircleView полностью удалён)

        final EditText textUsername = findViewById(R.id.textUsername);
        // --- Автозагрузка ключа и автологин ---
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        String savedKey = prefs.getString("user_key", "");
        boolean wasLoginSuccess = prefs.getBoolean("login_success", false);
        if (!savedKey.isEmpty() && wasLoginSuccess) {
            textUsername.setText(savedKey);
            textUsername.post(() -> performLogin(savedKey));
        }
        // --- конец автозагрузки ---
        // Сохранять текст при каждом изменении
        textUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                getSharedPreferences("login", MODE_PRIVATE)
                    .edit()
                    .putString("last_login", s.toString())
                    .apply();
            }
        });
    }




    @SuppressLint("ClickableViewAccessibility")
    private void setupRootTouchListener() {
        View rootView = findViewById(R.id.rootLayout);
        if (rootView != null) {
            rootView.setOnTouchListener((v, event) -> {
                // Защита от бесконечных событий ввода
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTouchTime < 16) { // ~60 FPS
                        return true; // Поглощаем событие
                    }
                    lastTouchTime = currentTime;
                }

                View currentFocus = getCurrentFocus();
                if (currentFocus != null) {
                    currentFocus.clearFocus();
                    currentFocus.setFocusable(false);
                    currentFocus.setFocusableInTouchMode(false);
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    }
                    FrameLayout passwordLayout = findViewById(R.id.passwordLayout);

                }
                if (currentFocus != null) {
                    currentFocus.setFocusable(true);
                    currentFocus.setFocusableInTouchMode(true);
                }
                return false;
            });
        }
    }

    private void initView() {
        final Context mContext = this;
        final EditText textUsername = findViewById(R.id.textUsername);

        textUsername.setTransformationMethod(PasswordTransformationMethod.getInstance());

        FrameLayout passwordLayout = findViewById(R.id.passwordLayout);

        textUsername.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ImageView warningIcon = findViewById(R.id.icon);
                if (warningIcon != null) {
                    warningIcon.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        String userKey = prefs.read(USER, "");
        if (userKey != null) {
            textUsername.setText(userKey);
        }

        ImageView btnSignIn = findViewById(R.id.btnSignIn);


        TextView getKeyText = findViewById(R.id.getKeyText);
        getKeyText.setOnClickListener(v -> {
            String url = GetKey();
            String[] telegramPackages = {"org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram", "org.telegram.plus"};

            boolean opened = false;

            for (String packageName : telegramPackages) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setPackage(packageName);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                    opened = true;
                    break;
                } catch (android.content.ActivityNotFoundException ignored) {
                }
            }

            if (!opened) {
                try {
                    Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(fallbackIntent);
                } catch (Exception e) {
                    Toast.makeText(LoginActivity.this, getString(R.string.cannot_open_link), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Add click listener for Telegram button
        ImageView telegramButton = findViewById(R.id.telegramButton);
        if (telegramButton != null) {
            telegramButton.setOnClickListener(v -> {
                String url = GetKey();
                String[] telegramPackages = {"org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram", "org.telegram.plus"};

                boolean opened = false;

                for (String packageName : telegramPackages) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setPackage(packageName);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(intent);
                        opened = true;
                        break;
                    } catch (android.content.ActivityNotFoundException ignored) {
                    }
                }

                if (!opened) {
                    try {
                        Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        fallbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(fallbackIntent);
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, getString(R.string.cannot_open_link), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        textUsername.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnSignIn.setEnabled(!s.toString().trim().isEmpty());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
        textUsername.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                btnSignIn.performClick();
                return true;
            }
            return false;
        });
        btnSignIn.setEnabled(!textUsername.getText().toString().trim().isEmpty());

        btnSignIn.setOnClickListener(v -> {
            String inputKey = textUsername.getText().toString().trim();
            if (inputKey.isEmpty()) {
                textUsername.setError("Field cannot be empty");
                return;
            }

            resetProgressUI();

            btnSignIn.setEnabled(false);
            View mainContent = findViewById(R.id.mainContent);
            if (mainContent != null) {
                mainContent.setEnabled(false);
            }

            prefs.write(USER, inputKey);
            USERKEY = inputKey;

            View rootLayout = findViewById(R.id.rootLayout);
            if (rootLayout != null) {
                rootLayout.setEnabled(false);
                setAllEnabled(rootLayout, false);
            }

            View progressOverlay = findViewById(R.id.progressOverlay);
            if (progressOverlay != null) {
                progressOverlay.setVisibility(View.VISIBLE);
                progressOverlay.setAlpha(1f);
            }

            BlurView blurViewOverlay = findViewById(R.id.blurViewOverlay);
            if (blurViewOverlay != null) {
                blurViewOverlay.setVisibility(View.VISIBLE);
                blurViewOverlay.setAlpha(1f);
            }

            Login(this, inputKey);
        });
    }

    private static void setProgressSmoothly(ProgressBar progressBar, int progress) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), progress);
        animation.setDuration(300);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private void animateBackImages() {
        ImageView back = findViewById(R.id.back);
        ImageView back2 = findViewById(R.id.back2);

        if (back == null || back2 == null) return;

        float screenHeight = getResources().getDisplayMetrics().heightPixels;
        float verticalShift = screenHeight * 0.08f;


        ObjectAnimator backUpDown = ObjectAnimator.ofFloat(back, View.TRANSLATION_Y, 0f, -verticalShift, 0f);
        backUpDown.setDuration(9000);
        backUpDown.setRepeatMode(ValueAnimator.REVERSE);
        backUpDown.setRepeatCount(ValueAnimator.INFINITE);
        backUpDown.setInterpolator(new DecelerateInterpolator());
        backUpDown.start();


        ObjectAnimator back2UpDown = ObjectAnimator.ofFloat(back2, View.TRANSLATION_Y, 0f, verticalShift, 0f);
        back2UpDown.setDuration(9500);
        back2UpDown.setRepeatMode(ValueAnimator.REVERSE);
        back2UpDown.setRepeatCount(ValueAnimator.INFINITE);
        back2UpDown.setInterpolator(new DecelerateInterpolator());
        back2UpDown.start();


        float dp40 = 40f * getResources().getDisplayMetrics().density;
        ValueAnimator backHorizontalAnimator = ValueAnimator.ofFloat(-dp40, dp40);
        backHorizontalAnimator.setDuration(12000);
        backHorizontalAnimator.setRepeatMode(ValueAnimator.REVERSE);
        backHorizontalAnimator.setRepeatCount(ValueAnimator.INFINITE);
        backHorizontalAnimator.setInterpolator(new DecelerateInterpolator());
        backHorizontalAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            back.setTranslationX(value);
        });
        backHorizontalAnimator.start();


        float dpOffset = 200f * getResources().getDisplayMetrics().density;
        ValueAnimator back2HorizontalAnimator = ValueAnimator.ofFloat(dpOffset - dp40, dpOffset + dp40);
        back2HorizontalAnimator.setDuration(13000);
        back2HorizontalAnimator.setRepeatMode(ValueAnimator.REVERSE);
        back2HorizontalAnimator.setRepeatCount(ValueAnimator.INFINITE);
        back2HorizontalAnimator.setInterpolator(new DecelerateInterpolator());
        back2HorizontalAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            back2.setTranslationX(value);
        });
        back2HorizontalAnimator.start();
    }


    private void applyGoldGradientToText(TextView textView) {
        if (textView == null) return;
        final float width = textView.getPaint().measureText(textView.getText().toString());
        final float textSize = textView.getTextSize();
        final int[] colors = new int[]{
                Color.parseColor("#2196F3"), // основной синий
                Color.parseColor("#AEEFFF"), // fade-in светло-синий
                Color.WHITE, // белый блик
                Color.parseColor("#AEEFFF"), // fade-out светло-синий
                Color.parseColor("#2196F3") // основной синий
        };
        final float[] positions = new float[]{0f, 0.35f, 0.5f, 0.65f, 1f};

        ValueAnimator animator = ValueAnimator.ofFloat(-0.75f, 1.75f);
        animator.setDuration(16000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            float highlightCenter = width * animatedValue;
            float highlightWidth = width * 1.5f;
            Shader shader = new LinearGradient(
                    highlightCenter - highlightWidth / 2, 0, highlightCenter + highlightWidth / 2, textSize,
                    colors, positions, Shader.TileMode.CLAMP);
            textView.getPaint().setShader(shader);
            textView.invalidate();
        });
        animator.start();
    }


    private void applyBlueGradientToText(TextView textView) {
        if (textView == null) return;
        final float width = textView.getPaint().measureText(textView.getText().toString());
        final float textSize = textView.getTextSize();
        final int[] colors = new int[]{
            Color.parseColor("#2196F3"),
            Color.parseColor("#AEEFFF"),
            Color.WHITE,
            Color.parseColor("#AEEFFF"),
            Color.parseColor("#2196F3")
        };
        final float[] positions = new float[]{0f, 0.35f, 0.5f, 0.65f, 1f};
        ValueAnimator animator = ValueAnimator.ofFloat(-0.75f, 1.75f);
        animator.setDuration(6000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            float highlightCenter = width * animatedValue;
            float highlightWidth = width * 1.5f;
            Shader shader = new LinearGradient(
                highlightCenter - highlightWidth / 2, 0, highlightCenter + highlightWidth / 2, textSize,
                colors, positions, Shader.TileMode.CLAMP);
            textView.getPaint().setShader(shader);
            textView.invalidate();
        });
        animator.start();
    }

    private void applyWhiteGradientToText(TextView textView) {
        if (textView == null) return;
        final float width = textView.getPaint().measureText(textView.getText().toString());
        final float textSize = textView.getTextSize();
        final int[] colors = new int[]{
            Color.WHITE,
            Color.parseColor("#bdbdbd"), // светло-серый блик
            Color.parseColor("#757575"), // тёмный блик
            Color.parseColor("#bdbdbd"),
            Color.WHITE
        };
        final float[] positions = new float[]{0f, 0.35f, 0.5f, 0.65f, 1f};
        ValueAnimator animator = ValueAnimator.ofFloat(-0.75f, 1.75f);
        animator.setDuration(3500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            float highlightCenter = width * animatedValue;
            float highlightWidth = width * 1.5f;
            Shader shader = new LinearGradient(
                highlightCenter - highlightWidth / 2, 0, highlightCenter + highlightWidth / 2, textSize,
                colors, positions, Shader.TileMode.CLAMP);
            textView.getPaint().setShader(shader);
            textView.invalidate();
        });
        animator.start();
    }


    @SuppressLint("SdCardPath")
    public static void Login(final LoginActivity mContext, final String userKey) {
        @SuppressLint("HandlerLeak") final Handler loginHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ImageView btnSignIn = mContext.findViewById(R.id.btnSignIn);
                View rootLayout = mContext.findViewById(R.id.rootLayout);
                View mainContent = mContext.findViewById(R.id.mainContent);
                View progressOverlay = mContext.findViewById(R.id.progressOverlay);
                ProgressBar progressBar = mContext.progressBar;
                ImageView progressLogo = mContext.findViewById(R.id.progressLogo);
                BlurView blurViewOverlay = mContext.findViewById(R.id.blurViewOverlay);

                if (msg.what == 0) {
                    // Успешная авторизация (VALID)
                    // Сохраняем ключ и флаг успеха
                    SharedPreferences prefs = mContext.getSharedPreferences("login_prefs", MODE_PRIVATE);
                    prefs.edit().putString("user_key", userKey).putBoolean("login_success", true).apply();
                    // (Toast для успешного логина не нужен)
                    if (progressLogo != null && progressBar != null) {
                        progressBar.animate().alpha(0f).setDuration(300).start();
                        progressLogo.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                            progressLogo.setImageResource(R.drawable.tick_circle);
                            progressLogo.setAlpha(0f);
                            progressLogo.setVisibility(View.VISIBLE);
                            progressLogo.animate().alpha(1f).setDuration(300).withEndAction(() -> {
                                new Handler().postDelayed(() -> {
                                    animateHideAll(progressBar, progressLogo, blurViewOverlay, progressOverlay);

                                    // Переходим к MainActivity
                                    MainActivity.goMain(mContext);

                                    if (mContext instanceof android.app.Activity) {
                                        ((android.app.Activity) mContext).overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out);
                                    }
                                    mContext.finishActivity(0);
                                }, 2500);
                            }).start();
                        }).start();
                    }
                } else if (msg.what == 1) {
                    // Ошибка авторизации
                    if (progressLogo != null && progressBar != null) {
                        progressBar.animate().alpha(0f).setDuration(300).start();
                        progressLogo.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                            progressLogo.setImageResource(R.drawable.minus_cirlce);
                            progressLogo.setAlpha(0f);
                            progressLogo.setVisibility(View.VISIBLE);
                            progressLogo.animate().alpha(1f).setDuration(300).withEndAction(() -> {
                                new Handler().postDelayed(() -> {
                                    animateHideAll(progressBar, progressLogo, blurViewOverlay, progressOverlay);
                                    if (btnSignIn != null) btnSignIn.setEnabled(true);
                                    if (rootLayout != null) {
                                        rootLayout.setEnabled(true);
                                        mContext.setAllEnabled(rootLayout, true);
                                    }
                                    if (mainContent != null) mainContent.setEnabled(true);

                                    progressBar.setProgress(0);
                                    progressBar.setAlpha(0f);
                                    progressBar.setVisibility(View.VISIBLE);
                                    progressBar.animate().alpha(1f).setDuration(400).start();
                                    if (blurViewOverlay != null) {
                                        blurViewOverlay.setAlpha(0f);
                                        blurViewOverlay.setVisibility(View.VISIBLE);
                                        blurViewOverlay.animate().alpha(1f).setDuration(400).start();
                                    }
                                    if (progressOverlay != null) {
                                        progressOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> progressOverlay.setVisibility(View.GONE)).start();
                                    }
                                }, 2500);
                            }).start();
                        }).start();
                    }
                    Toast.makeText(mContext, msg.obj.toString(), Toast.LENGTH_LONG).show();
                }
            }


            private void animateHideAll(ProgressBar progressBar, ImageView progressLogo, BlurView blurViewOverlay, View progressOverlay) {
                int duration = 500;

                if (progressBar != null) {
                    progressBar.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(duration).withEndAction(() -> {
                        progressBar.setVisibility(View.GONE);
                        progressBar.setScaleX(1f);
                        progressBar.setScaleY(1f);
                    }).start();
                }

                if (progressLogo != null) {
                    progressLogo.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(duration).withEndAction(() -> {
                        progressLogo.setVisibility(View.GONE);
                        progressLogo.setScaleX(1f);
                        progressLogo.setScaleY(1f);
                    }).start();
                }

                if (blurViewOverlay != null) {
                    blurViewOverlay.animate().alpha(0f).setDuration(duration).withEndAction(() -> blurViewOverlay.setVisibility(View.GONE)).start();
                }

                if (progressOverlay != null) {
                    progressOverlay.animate().alpha(0f).setDuration(duration).withEndAction(() -> progressOverlay.setVisibility(View.GONE)).start();
                }
            }
        };

        new Thread(() -> {

            for (int progress = 0; progress <= 100; progress += 10) {
                int currentProgress = progress;
                mContext.runOnUiThread(() -> {
                    if (mContext.progressBar != null) {
                        setProgressSmoothly(mContext.progressBar, currentProgress);
                    }
                });
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }

            // Получаем путь к CA сертификату
            String caPath = BoxApplication.copyCacertFromAssetsIfNeeded(mContext);

            String result = Check(mContext, userKey);

            
            Message msg = new Message();
            if (result != null && result.equals("OK")) {
                // Save key to JSON file
                try {
                    org.json.JSONObject json = new org.json.JSONObject();
                    json.put("licence", userKey);

                    java.io.File dir = new java.io.File(
                            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                            "Xproject"
                    );
                    if (!dir.exists()) {
                        boolean created = dir.mkdirs();
                    }

                    java.io.File jsonFile = new java.io.File(dir, "Licence.json");
                    if (!jsonFile.exists()) {
                        jsonFile.createNewFile();
                    }

                    java.io.FileWriter writer = new java.io.FileWriter(jsonFile, false);
                    writer.write(json.toString());
                    writer.flush();
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                msg.what = 0;
            } else {
                msg.what = 1;
                msg.obj = result;
            }
            loginHandler.sendMessage(msg);
        }).start();
    }

    private void resetProgressUI() {
        ProgressBar progressBar = findViewById(R.id.circularProgressBar);
        ImageView progressLogo = findViewById(R.id.progressLogo);

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setAlpha(1f);
            progressBar.setProgress(0);
        }

        if (progressLogo != null) {
            progressLogo.setVisibility(View.VISIBLE);
            progressLogo.setAlpha(1f);
            progressLogo.setImageResource(R.drawable.logo);
        }
    }

    private static native String Check(Context mContext, String userKey);

    public static native String GetKey();

    // Method to get current license
    public static String getCurrentLicense() {
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
                return json.getString("licence");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            OverlayPermision();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (android.provider.Settings.canDrawOverlays(this)) {
                InstllUnknownApp();
            }
        } else if (requestCode == REQUEST_MANAGE_UNKNOWN_APP_SOURCES) {
            if (isPermissionGaranted()) {
                takeFilePermissions();
            }
        }
    }

    private void setAllEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                setAllEnabled(viewGroup.getChildAt(i), enabled);
            }
        }
    }

    /**
     * Проверяет, что все необходимые разрешения предоставлены
     */
    private boolean areAllPermissionsGranted() {
        try {
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

            // android.util.Log.d("LoginActivity", "Permissions check: storage=" + storagePermission + 
            //         ", install=" + installPermission + ", overlay=" + overlayPermission + 
            //         ", manageFiles=" + manageFilesPermission);

            return storagePermission && installPermission && overlayPermission && manageFilesPermission;
        } catch (Exception e) {
            // android.util.Log.e("LoginActivity", "Error checking permissions: " + e.getMessage());
            return false;
        }
    }

    /**
     * Проверяет, что пользователь уже авторизован
     */
    private boolean isUserAuthorized() {
        // Проверяем инициализацию prefs
        if (prefs == null) {
            // android.util.Log.d("LoginActivity", "Prefs not initialized");
            return false;
        }
        
        // Проверяем сохраненный ключ пользователя
        String userKey = prefs.read(USER, "");
        if (userKey == null || userKey.isEmpty()) {
            // android.util.Log.d("LoginActivity", "No user key found");
            return false;
        }

        // Проверяем файл лицензии
        try {
            java.io.File dir = new java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                    "Xproject"
            );
            java.io.File jsonFile = new java.io.File(dir, "Licence.json");
            if (!jsonFile.exists()) {
                // android.util.Log.d("LoginActivity", "License file not found");
                return false;
            }

            // Проверяем SharedPreferences для флага успешного логина
            android.content.SharedPreferences loginPrefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
            boolean loginSuccess = loginPrefs.getBoolean("login_success", false);
            String savedUserKey = loginPrefs.getString("user_key", "");

            if (!loginSuccess || !userKey.equals(savedUserKey)) {
                // android.util.Log.d("LoginActivity", "Login not successful or keys don't match");
                return false;
            }

            // android.util.Log.d("LoginActivity", "User authorized with key: " + userKey);
            return true;
        } catch (Exception e) {
            // android.util.Log.e("LoginActivity", "Error checking authorization: " + e.getMessage());
            return false;
        }
    }

    private void animatePasswordLayout() {
        FrameLayout passwordLayout = findViewById(R.id.passwordLayout);
        if (passwordLayout == null) return;


        passwordLayout.setAlpha(0f);
        passwordLayout.setScaleX(0.9f);
        passwordLayout.setScaleY(0.9f);
        passwordLayout.setTranslationY(40f);

        passwordLayout.post(() -> {
            passwordLayout.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setStartDelay(100).setDuration(500).setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator()).start();
        });
    }

    private boolean isFridaDetected() {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("/proc/" + android.os.Process.myPid() + "/maps"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("frida") || line.contains("gadget")) {
                    return true;
                }
            }
            reader.close();
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Detects HTTP proxy or known HTTP sniffer apps (e.g., HTTP Canary).
     */
    private boolean isHttpSnifferDetected() {

        String[] sniffers = {"com.guoshi.httpcanary", "com.chauncy.vpn", "org.webrtc.vpn", "com.xproxy.network",};
        PackageManager pm = getPackageManager();
        for (String pkg : sniffers) {
            try {
                pm.getPackageInfo(pkg, 0);
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        String proxyHost = System.getProperty("http.proxyHost");
        return proxyHost != null && !proxyHost.isEmpty();
    }


    private static void executeShell(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            process.waitFor();
        } catch (Exception ignored) {
        }
    }


    private static void deleteDirectory(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            java.io.File[] children = dir.listFiles();
            if (children != null) {
                for (java.io.File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        if (dir != null) {
            boolean delete = dir.delete();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        

        
  //      stopOverlaySockMonitor();
        
        // Полная очистка при закрытии LoginActivity
        cleanupOnAppExit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Для планшетов разрешаем поворот экрана
        boolean isTablet = DeviceUtils.isTablet(this);
        if (isTablet) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }



    // Ensure WRITE_EXTERNAL_STORAGE permission is handled at runtime for Android ≤ 29
    @Override
    protected void onStart() {
        super.onStart();

        if (android.os.Build.VERSION.SDK_INT <= 29) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1010);
            }
        }
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
        
        // Для планшетов разрешаем поворот экрана
        boolean isTablet = DeviceUtils.isTablet(this);
        if (isTablet) {
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            
            // Пересчитываем размер mainContent при повороте экрана
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                View mainContent = findViewById(R.id.mainContent);
                if (mainContent != null) {
                    // Получаем обновленные размеры экрана
                    android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    
                    // Вычисляем размер (берем меньшее из ширины или высоты экрана)
                    int screenSize = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
                    
                    // Устанавливаем квадратную форму для mainContent
                    ViewGroup.LayoutParams mainContentParams = mainContent.getLayoutParams();
                    mainContentParams.width = screenSize;
                    mainContentParams.height = screenSize;
                    mainContent.setLayoutParams(mainContentParams);
                    
                    // Центрируем mainContent
                    if (mainContent.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                        ((FrameLayout.LayoutParams) mainContent.getLayoutParams()).gravity = android.view.Gravity.CENTER;
                    }
                }
            }, 100); // Небольшая задержка для стабилизации
        }
    }

    /**
     * Updates the "time" field in Licence.json to the current system time.
     */
    private void updateLicenceTime() {
        try {
            java.io.File dir = new java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                    "Xproject"
            );
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
            }

            java.io.File jsonFile = new java.io.File(dir, "Licence.json");
            if (jsonFile.exists()) {
                java.io.FileReader reader = new java.io.FileReader(jsonFile);
                char[] buffer = new char[(int) jsonFile.length()];
                reader.read(buffer);
                reader.close();

                String content = new String(buffer);
                org.json.JSONObject json = new org.json.JSONObject(content);
                json.put("time", System.currentTimeMillis());

                java.io.FileWriter writer = new java.io.FileWriter(jsonFile, false);
                writer.write(json.toString());
                writer.flush();
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // --- OVERLAY AND SOCK MONITORING ---
    private Handler overlaySockHandler = new Handler();
    private Runnable overlaySockRunnable;
    private boolean overlaySockMonitorActive = false;
    
    
    
    private void cleanupOnAppExit() {
        try {
            
            // Останавливаем overlay сервис
            try {
                // com.glass.engine.floating.Overlay.forceStopOverlay(); // Удалено
            } catch (Exception e) {
            }
            
            // Убиваем sock cheat
            try {
             //   killSockCheatProcess();
            } catch (Exception e) {
            }
            

            
            // Сбрасываем флаги в ESPView
            try {
              //  com.glass.engine.floating.ESPView.setGameActive(false);
               // com.glass.engine.floating.ESPView.disableOverlay();
            } catch (Exception e) {
            }
            
            
        } catch (Exception e) {
        }
    }

    // Функция автологина
    private void performLogin(String key) {
        EditText editTextKey = findViewById(R.id.textUsername);
        ImageView btnSignIn = findViewById(R.id.btnSignIn);
        if (editTextKey != null) {
            editTextKey.setText(key);
        }
        if (btnSignIn != null) {
            btnSignIn.performClick();
        }
    }
}