package com.glass.engine.fragments;

import static com.blankj.molihuan.utilcode.util.ServiceUtils.startService;
import static com.blankj.molihuan.utilcode.util.ServiceUtils.stopService;

import android.content.Intent;
import android.content.SharedPreferences;


import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.glass.engine.R;
import com.glass.engine.activity.LoadingActivity;
import com.glass.engine.BoxApplication;
 
import com.topjohnwu.superuser.Shell;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
// Add this import at the top with other imports
import android.util.Log;
import android.animation.ValueAnimator;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Color;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import eightbitlab.com.blurview.BlurView;
import com.vbox.VBoxCore;
import android.view.ViewOutlineProvider;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.view.ViewConfiguration;
import android.media.projection.MediaProjectionManager;
import android.provider.Settings;


public class GameInfoFragment extends Fragment {
    // Флаг для отслеживания состояния слайдера
    private boolean isLaunched = false;
    private static String gamepkg = "";
private static final int USER_ID = 0; // BlackBox default user
    // BroadcastReceiver для запуска bypass по сигналу от ColorPickerService
    private android.content.BroadcastReceiver bypassTriggerReceiver;

    static {
        try {
            System.loadLibrary("client");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    
     
    // --- SHIMMER-ЭФФЕКТ ДЛЯ АКТИВНОЙ КНОПКИ ---
    private final java.util.Map<TextView, ValueAnimator> shimmerAnimators = new java.util.HashMap<>();
    private void applyShimmerToButton(final TextView button, String type) {
        ValueAnimator old = shimmerAnimators.get(button);
        if (old != null) old.cancel();
        button.getPaint().setShader(null);
        button.invalidate();
        button.post(() -> {
            final int width = button.getWidth();
            final int height = button.getHeight();
            int[] colors;
            switch (type) {
                case "basic":
                    colors = new int[]{
                        android.graphics.Color.parseColor("#43EA7F"),
                        android.graphics.Color.parseColor("#B6FFB0"),
                        android.graphics.Color.parseColor("#43EA7F")
                    };
                    break;
                case "advanced":
                    colors = new int[]{
                        android.graphics.Color.parseColor("#FFD600"),
                        android.graphics.Color.parseColor("#FFF59D"),
                        android.graphics.Color.parseColor("#FFD600")
                    };
                    break;
                case "ultimate":
                    colors = new int[]{
                        android.graphics.Color.parseColor("#FF5252"),
                        android.graphics.Color.parseColor("#FF8A80"),
                        android.graphics.Color.parseColor("#FF5252")
                    };
                    break;
                default:
                    colors = new int[]{
                        android.graphics.Color.parseColor("#AEEFFF"),
                        android.graphics.Color.WHITE,
                        android.graphics.Color.parseColor("#AEEFFF")
                    };
            }
            final float[] positions = new float[]{0f, 0.5f, 1f};
            ValueAnimator animator = ValueAnimator.ofFloat(-1f, 2f);
            animator.setDuration(1800);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(animation -> {
                float animatedValue = (float) animation.getAnimatedValue();
                float center = width * animatedValue;
                android.graphics.Shader shader = new android.graphics.LinearGradient(
                    center - width / 2, 0, center + width / 2, height,
                    colors, positions, android.graphics.Shader.TileMode.CLAMP
                );
                button.getPaint().setShader(shader);
                button.invalidate();
            });
            animator.start();
            shimmerAnimators.put(button, animator);
        });
    }
    private void clearShimmer(TextView button) {
        ValueAnimator old = shimmerAnimators.get(button);
        if (old != null) old.cancel();
        button.getPaint().setShader(null);
        button.invalidate();
    }

    private void animateButtonBackground(final TextView button, int newResId) {
        Drawable oldBg = button.getBackground();
        Drawable newBg = button.getContext().getDrawable(newResId);
        if (oldBg == null || newBg == null) {
            button.setBackgroundResource(newResId);
            return;
        }
        android.graphics.drawable.TransitionDrawable transition = new android.graphics.drawable.TransitionDrawable(new Drawable[]{oldBg, newBg});
        button.setBackground(transition);
        transition.startTransition(250);
    }


    public static GameInfoFragment newInstance(
            String gamenameincard,
            String gameversionincard,
            int gamecardimage,
            int gamecardoverlayimage,
            int fragmentimage,
            int gameIcon,
            String fragmentgamename,
            String fragmentpublisher,
            String fragmentdescription,
            String basicDescription,
            String advancedDescription,
            String ultimateDescription,
            String gamepackagename) {
        GameInfoFragment fragment = new GameInfoFragment();
        Bundle args = new Bundle();
        args.putString("gamenameincard", gamenameincard);
        args.putString("gameversionincard", gameversionincard);
        args.putInt("gamecardimage", gamecardimage);
        args.putInt("gamecardoverlayimage", gamecardoverlayimage);
        args.putInt("fragmentimage", fragmentimage);
        args.putInt("gameIcon", gameIcon);
        args.putString("fragmentgamename", fragmentgamename);
        args.putString("fragmentpublisher", fragmentpublisher);
        args.putString("fragmentdescription", fragmentdescription);
        args.putString("basicDescription", basicDescription);
        args.putString("advancedDescription", advancedDescription);
        args.putString("ultimateDescription", ultimateDescription);
        args.putString("gamepackagename", gamepackagename);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Сбрасываем все флаги при создании фрагмента
        isLaunched = false;
        android.util.Log.d("GameInfoFragment", "🔄 onCreateView - resetting all flags to clean state");
                 
        // Затемнение до появления контента
        View preRoot = new View(requireContext());
        preRoot.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        preRoot.setBackgroundColor(android.graphics.Color.BLACK);
        preRoot.setAlpha(1f);

        ViewGroup rootContainer = (ViewGroup) requireActivity().findViewById(android.R.id.content);
        rootContainer.addView(preRoot);

        // Продолжим загрузку фрагмента
        View view = inflater.inflate(R.layout.fragment_game_info, container, false);

        // Установить фоновое изображение, если передано
        ImageView backgroundImage = view.findViewById(R.id.background_image);
        Bundle argsForBg = getArguments();
        int imageResIdForBg = 0;
        if (argsForBg != null) {
            imageResIdForBg = argsForBg.getInt("fragmentimage", 0);
        }
        if (backgroundImage != null && imageResIdForBg != 0) {
            try {
                // Безопасная загрузка изображения
                android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeResource(getResources(), imageResIdForBg, options);

                // Проверяем размер изображения
                int imageHeight = options.outHeight;
                int imageWidth = options.outWidth;
                long imageSize = (long) imageWidth * imageHeight * 4; // 4 bytes per pixel (ARGB)

                // Если изображение слишком большое, сжимаем его
                if (imageSize > 50 * 1024 * 1024) { // 50MB limit
                    options.inSampleSize = calculateInSampleSize(options, 1920, 1080);
                    options.inJustDecodeBounds = false;
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeResource(getResources(), imageResIdForBg, options);
                    if (bitmap != null) {
                        backgroundImage.setImageBitmap(bitmap);
                    }
                } else {
                    // Изображение нормального размера
                    backgroundImage.setImageResource(imageResIdForBg);
                }
            } catch (OutOfMemoryError e) {
                // Если не хватает памяти, используем стандартное изображение
                backgroundImage.setImageResource(R.drawable.fragment_delta);
            } catch (Exception e) {
                // В случае любой ошибки используем стандартное изображение
                backgroundImage.setImageResource(R.drawable.fragment_delta);
            }
        }

        // Запустим исчезновение затмения после небольшой задержки
        preRoot.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction(() -> rootContainer.removeView(preRoot))
            .start();

        // Убираем системные отступы и растягиваем Fragment под статус-бар
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> WindowInsetsCompat.CONSUMED);
        view.setFitsSystemWindows(false);

        // Программно определяем высоту навигационной панели и устанавливаем отступ
        View bottomCardBlock = view.findViewById(R.id.bottom_card_block);
        if (bottomCardBlock != null) {
            // Ждем когда view будет измерен
            bottomCardBlock.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // Убираем listener после первого вызова
                    bottomCardBlock.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Определяем высоту навигационной панели
                    int navigationBarHeight = getNavigationBarHeight();

                    // Устанавливаем отступ
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomCardBlock.getLayoutParams();
                    if (params != null) {
                        // Базовый отступ 24dp + высота навигационной панели
                        int basePadding = (int) (24 * getResources().getDisplayMetrics().density);
                        params.bottomMargin = basePadding + navigationBarHeight;
                        bottomCardBlock.setLayoutParams(params);

                        android.util.Log.d("GameInfoFragment", "🔧 Navigation bar height: " + navigationBarHeight + "px, Total bottom margin: " + params.bottomMargin + "px");
                    }
                }
            });
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            requireActivity().getWindow().getAttributes().layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().getWindow().setDecorFitsSystemWindows(false);
            // Не скрываем status bar, чтобы не убирать ярлыки
        }


        // Удалены: bottomButtonRow, installButton, installSlider, contentFull

        View imageView = view.findViewById(R.id.fragmentimage);
        ViewCompat.setOnApplyWindowInsetsListener(imageView, (v, insets) -> WindowInsetsCompat.CONSUMED);

        TextView gameNameText = view.findViewById(R.id.fragmentgamename);
        TextView publisherText = view.findViewById(R.id.fragmentpublisher);
        TextView descriptionText = view.findViewById(R.id.fragmentdescription);

        // Кастомный слайдер
        FrameLayout sliderContainer = view.findViewById(R.id.custom_slider_container);
        View sliderTrack = view.findViewById(R.id.custom_slider_track);
        ImageView sliderThumb = view.findViewById(R.id.btnSignIn);
        TextView sliderLabel = view.findViewById(R.id.slider_label);
        // TextView testLabel = view.findViewById(R.id.test_gradient_label); // <--- удалено

        sliderContainer.post(() -> {
            final int containerWidth = sliderContainer.getWidth();
            final int thumbWidth = sliderThumb.getWidth();
            final int thumbMargin = (int) (sliderContainer.getResources().getDisplayMetrics().density * 8); // 8dp
            final int minX = thumbMargin;
            final int maxX = containerWidth - thumbWidth - thumbMargin;

            sliderThumb.setY((sliderContainer.getHeight() - sliderThumb.getHeight()) / 2f); // Центрируем по вертикали
            sliderThumb.setX(minX); // Центрируем по горизонтали с учётом отступа

            sliderThumb.setOnTouchListener(new View.OnTouchListener() {
                float dX;
                int[] containerLocation = new int[2];
                android.os.Handler resetHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                Runnable resetRunnable = null;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    sliderContainer.getLocationOnScreen(containerLocation);
                    int containerX = containerLocation[0];
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            android.util.Log.d("GameInfoFragment", "Slider: ACTION_DOWN");
                            dX = v.getX() - (event.getRawX() - containerX);
                            // Отменяем предыдущий сброс если он был
                            if (resetRunnable != null) {
                                resetHandler.removeCallbacks(resetRunnable);
                                resetRunnable = null;
                            }
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float newX = event.getRawX() - containerX + dX;
                            if (newX < minX) newX = minX;
                            if (newX > maxX) newX = maxX;
                            // Прямое движение для мгновенного следования за пальцем
                            v.setX(newX);
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            float finalX = v.getX();
                            float threshold = maxX - (maxX - minX) * 0.3f; // 70% от пути

                            if (finalX >= threshold && !isLaunched) {
                                // Проверяем режим слайдера
                                TextView sliderLabel = requireView().findViewById(R.id.slider_label);
                                boolean isStopMode = sliderLabel != null && "slide to stop".equals(sliderLabel.getText().toString());

                                if (isStopMode) {
                                    // Режим остановки - останавливаем все сервисы
                                    android.util.Log.d("GameInfoFragment", "Slider: Threshold reached → STOP mode, stopping services");

                                    // Анимация движения до конца
                                    v.animate()
                                        .x(maxX)
                                        .setDuration(200)
                                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                        .withEndAction(() -> {
                                            // Останавливаем все сервисы
                                            stopAllServices();

                                            // Возвращаем слайдер в начальное положение
                                            v.animate()
                                                .x(minX)
                                                .setDuration(300)
                                                .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                                                .withEndAction(() -> {
                                                    // Переключаем обратно в режим запуска
                                                    isLaunched = false;

                                                    // Возвращаем текст "slide to launch"
                                                    sliderLabel.setText(getString(R.string.slide_to_launch));
                                                    sliderLabel.setTextColor(android.graphics.Color.WHITE);
                                                    sliderLabel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                                                    applyWhiteGradientToText(sliderLabel);

                                                    android.util.Log.d("GameInfoFragment", "✅ Services stopped, slider returned to LAUNCH mode");
                                                })
                                                .start();
                                        })
                                        .start();

                                } else {
                                    // Режим запуска - запускаем игру
                                    android.util.Log.d("GameInfoFragment", "Slider: Threshold reached → LAUNCH mode, starting game");

                                    // СРАЗУ делаем экран черным когда слайдер достигает 100%
                            //        makeScreenBlack();

                                    // Анимация движения до конца
                                    v.animate()
                                        .x(maxX)
                                        .setDuration(200)
                                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                        .withEndAction(() -> {
                                        // Запуск после достижения конца
                                        isLaunched = true;
                                        Bundle args = getArguments();
                                        if (args != null) {
                                            String packageName = args.getString("gamepackagename", "");
                                            if (!packageName.isEmpty()) {
                                                boolean isRootMode = false;
                                                try {
                                                    isRootMode = false;
                                                } catch (Exception e) {
                                                    isRootMode = false;
                                                }
                                                android.util.Log.d("GameInfoFragment", "Stage A: Mode check → " + (isRootMode ? "ROOT" : "NON-ROOT") + ", package=" + packageName);
                                                if (!isRootMode) {
                                                    boolean isInnerInstalled = false;
                                                    try {
                                                        isInnerInstalled = VBoxCore.get().isInstalled(packageName, USER_ID);
                                                    } catch (Exception e) {
                                                        isInnerInstalled = false;
                                                    }
                                                    android.util.Log.d("GameInfoFragment", "Stage A1: Meta installation status → " + (isInnerInstalled ? "installed" : "not installed"));
                                                    if (!isInnerInstalled) {
                                                        Toast.makeText(requireContext(), getString(R.string.app_not_installed_in_virtual_space_installing), Toast.LENGTH_SHORT).show();
                                                        sliderLabel.setText(getString(R.string.installing));
                                                        sliderLabel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                                                        applyWhiteGradientToText(sliderLabel);
                                                        sliderThumb.setEnabled(false);
                                                        android.util.Log.d("GameInfoFragment", "Stage A1.1: Cloning app into Meta...");
                                                        new Thread(() -> {
                                                            try {
                                                                VBoxCore.get().installPackageAsUser(packageName, USER_ID);
                                                                requireActivity().runOnUiThread(() -> {
                                                                    sliderThumb.setEnabled(true);
                                                                    sliderThumb.animate()
                                                                        .x(minX)
                                                                        .setDuration(400)
                                                                        .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                                                                        .start();
                                                                    sliderLabel.setText(getString(R.string.slide_to_launch));
                                                                    sliderLabel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                                                                    applyWhiteGradientToText(sliderLabel);
                                                                    Toast.makeText(requireContext(), getString(R.string.app_installed_in_virtual_space), Toast.LENGTH_SHORT).show();
                                                                    android.util.Log.d("GameInfoFragment", "Stage A1.1: ✅ Clone complete");
                                                                    VBoxCore.get().launchApk(packageName, USER_ID);
                                                                    isLaunched = false;
                                                                });
                                                            } catch (Exception e) {
                                                                requireActivity().runOnUiThread(() -> {
                                                                    sliderThumb.setEnabled(true);
                                                                    sliderThumb.animate()
                                                                        .x(minX)
                                                                        .setDuration(400)
                                                                        .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                                                                        .start();
                                                                    sliderLabel.setText(getString(R.string.install_failed_text));
                                                                    Toast.makeText(requireContext(), getString(R.string.install_failed), Toast.LENGTH_SHORT).show();
                                                                    android.util.Log.e("GameInfoFragment", "Stage A1.1: ❌ Clone failed: " + e.getMessage());
                                                                    isLaunched = false;
                                                                });
                                                            }
                                                        }).start();
                                                    } else {
                                                        // Уже установлен — прямой запуск без LoadingActivity
                                                        android.util.Log.d("GameInfoFragment", "Stage A1.2: Launching directly (Meta installed)");
                                                        VBoxCore.get().launchApk(packageName, USER_ID);
                                                    }
                                                } else {
                                                    // Root mode — прямой запуск без LoadingActivity
                                                    android.util.Log.d("GameInfoFragment", "Stage A2: Launching directly (ROOT mode)");
                                                    VBoxCore.get().launchApk(packageName, USER_ID);
                                                }
                                            }
                                        }
                                    })
                                    .start();
                                }
                            } else if (!isLaunched) {
                                // Если не достигли порога - возвращаем назад с анимацией
                                android.util.Log.d("GameInfoFragment", "Slider: Not enough distance → reset thumb");
                                v.animate()
                                    .x(minX)
                                    .setDuration(300)
                                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                                    .start();
                            }
                            return true;
                    }
                    return false;
                }
            });
        });

        // if (testLabel != null) {
        //     testLabel.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // <--- добавлено
        //     applyWhiteGradientToText(testLabel); // <--- добавлено
        // }

        ImageView backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            returnToMainLayout();
        });

        ImageView gameIcon = view.findViewById(R.id.game_icon);
        // --- КРУГЛАЯ ИКОНКА ДЛЯ ВСЕХ ANDROID ---
        android.graphics.drawable.Drawable drawable = gameIcon.getDrawable();
        if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
            android.graphics.Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) drawable).getBitmap();
            RoundedBitmapDrawable roundDrawable =
                    RoundedBitmapDrawableFactory.create(gameIcon.getResources(), bitmap);
            roundDrawable.setCircular(true);
            gameIcon.setImageDrawable(roundDrawable);
        }

        // --- RADIO КНОПКИ ЛОГИКА ---
        TextView radioBasic = view.findViewById(R.id.radio_basic);
        TextView radioAdvanced = view.findViewById(R.id.radio_advanced);
        TextView radioUltimate = view.findViewById(R.id.radio_ultimate);

        // Загрузить сохранённый выбор
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);
        String savedButton = prefs.getString(KEY_SELECTED_BUTTON, "basic"); // по умолчанию basic


        Bundle args = getArguments();
        if (args != null) {
            String gameNameInCard = args.getString("gamenameincard", "Unknown");
            String gameVersionInCard = args.getString("gameversionincard", "");
            int cardImageResId = args.getInt("gamecardimage", 0);
            int overlayImageResId = args.getInt("gamecardoverlayimage", 0);
            int imageResId = args.getInt("fragmentimage", 0);
            int gameIconResId = args.getInt("gameIcon", 0);
            String gameNameInFragment = args.getString("fragmentgamename", "Unknown");
            String publisher = args.getString("fragmentpublisher", "");
            String description = args.getString("fragmentdescription", "");
            String basicDescription = args.getString("basicDescription", "");
            String advancedDescription = args.getString("advancedDescription", "");
            String ultimateDescription = args.getString("ultimateDescription", "");
            String packageName = args.getString("gamepackagename", "");

            // Установить game_icon
            if (gameIconResId != 0) {
                gameIcon.setImageResource(gameIconResId);
                // Сделать круглым
                android.graphics.drawable.Drawable gameIconDrawable = gameIcon.getDrawable();
                if (gameIconDrawable instanceof android.graphics.drawable.BitmapDrawable) {
                    android.graphics.Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) gameIconDrawable).getBitmap();
                    RoundedBitmapDrawable roundDrawable =
                            RoundedBitmapDrawableFactory.create(gameIcon.getResources(), bitmap);
                    roundDrawable.setCircular(true);
                    gameIcon.setImageDrawable(roundDrawable);
                }
            }



            if (!packageName.isEmpty()) {
                boolean isInstalledOnDevice;
                try {
                    requireContext().getPackageManager().getPackageInfo(packageName, 0);
                    isInstalledOnDevice = true;
                } catch (Exception e) {
                    isInstalledOnDevice = false;
                }

                // Проверяем root-режим
                boolean isRootMode = false;
                try {
                    isRootMode = false;
                } catch (Exception e) {
                    isRootMode = false;
                }

                // В root-режиме всегда "Launch", в non-root проверяем Meta-установку
                if (isRootMode) {
                    if (isInstalledOnDevice) {
                        // installButton.setText("Launch"); // Removed
                    } else {
                        // installButton.setText("Install"); // Removed
                    }
                } else {
                    // Non-root режим - проверяем Meta-установку
                    boolean isInnerInstalled = VBoxCore.get().isInstalled(packageName, USER_ID);
                    if (!isInstalledOnDevice || !isInnerInstalled) {
                        // installButton.setText("Install"); // Removed
                    } else {
                        // installButton.setText("Launch"); // Removed
                    }
                }
            }

            if (publisherText != null) {
                publisherText.setText(publisher);
            }

            // Сохранить описания для использования в radioClickListener
            final String finalBasicDescription = basicDescription;
            final String finalAdvancedDescription = advancedDescription;
            final String finalUltimateDescription = ultimateDescription;

            if (descriptionText != null) {
                // Установить описание в зависимости от сохранённой кнопки
                String currentDescription = finalBasicDescription; // по умолчанию basic
                if ("advanced".equals(savedButton)) {
                    currentDescription = finalAdvancedDescription;
                } else if ("ultimate".equals(savedButton)) {
                    currentDescription = finalUltimateDescription;
                }
                descriptionText.setText(currentDescription);
            }

            if (gameNameText != null) {
                gameNameText.setText(gameNameInFragment);
            }

            int backgroundResId = imageResId;

            // imageView больше не ImageView, а View, поэтому не устанавливаем imageResId

            boolean isInstalledOnDevice = false;
            boolean isInnerInstalled = false;
            boolean isRootMode = false;
            try {
                isRootMode = false;
            } catch (Exception e) {
                isRootMode = false;
            }
            if (!isRootMode && packageName != null && !packageName.isEmpty()) {
                try {
                    isInnerInstalled = VBoxCore.get().isInstalled(packageName, USER_ID);
                } catch (Exception e) {
                    isInnerInstalled = false;
                }
            }
            String labelText;
            if (isRootMode) {
                            labelText = getString(R.string.slide_to_launch);
        } else {
            labelText = isInnerInstalled ? getString(R.string.slide_to_launch) : getString(R.string.slide_to_install);
            }
            sliderLabel.setText(labelText);
            sliderLabel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            applyWhiteGradientToText(sliderLabel);

            // Проверка установлен ли пакет
            if (packageName != null && !packageName.isEmpty()) {
                try {
                    requireContext().getPackageManager().getPackageInfo(packageName, 0);
                    isInstalledOnDevice = true;
                } catch (Exception e) {
                    isInstalledOnDevice = false;
                }
            }
            if (!isInstalledOnDevice) {
                if (sliderThumb != null) sliderThumb.setVisibility(View.GONE);
                if (sliderLabel != null) {
                    sliderLabel.setVisibility(View.VISIBLE);
                    sliderLabel.setText(getString(R.string.app_not_found_in_your_device));
                    sliderLabel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    sliderLabel.post(() -> {
                        ValueAnimator animator = ValueAnimator.ofFloat(-1f, 2f);
                        animator.setDuration(1800);
                        animator.setRepeatCount(ValueAnimator.INFINITE);
                        animator.setInterpolator(new android.view.animation.LinearInterpolator());
                        animator.addUpdateListener(animation -> {
                            float animatedValue = (float) animation.getAnimatedValue();
                            int width = sliderLabel.getWidth();
                            int height = sliderLabel.getHeight();
                            int[] colors = new int[]{
                                android.graphics.Color.parseColor("#FF5252"),
                                android.graphics.Color.parseColor("#FF8A80"),
                                android.graphics.Color.parseColor("#FF5252")
                            };
                            float[] positions = new float[]{0f, 0.5f, 1f};
                            android.graphics.Shader shader = new android.graphics.LinearGradient(
                                width * animatedValue, 0, width * (animatedValue + 1), height,
                                colors, positions, android.graphics.Shader.TileMode.CLAMP
                            );
                            sliderLabel.getPaint().setShader(shader);
                            sliderLabel.invalidate();
                        });
                        animator.start();
                    });
                }
            }


            // installButton.setOnClickListener(v -> { // Removed
            //     if (!packageName.isEmpty()) { // Removed
            //         boolean isInstalledOnDevice = false; // Removed
            //         try { // Removed
            //             requireContext().getPackageManager().getPackageInfo(packageName, 0); // Removed
            //             isInstalledOnDevice = true; // Removed
            //         } catch (Exception e) { // Removed
            //             isInstalledOnDevice = false; // Removed
            //         } // Removed
            //         // Removed
            //         if (!isInstalledOnDevice) { // Removed
            //             Toast.makeText(requireContext(), "App not installed on your device", Toast.LENGTH_SHORT).show(); // Removed
            //             return; // Removed
            //         } // Removed
            //         // Removed
            //         boolean isInnerInstalled = VBoxCore.get().isInstalled(packageName, USER_ID); // Removed
            //         // Removed
            //         Intent intent = new Intent(requireContext(), LoadingActivity.class); // Removed
            //         intent.putExtra("packageName", packageName); // Removed
            //         startActivity(intent); // Removed
            //         requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // Removed
            //     } else { // Removed
            //         Toast.makeText(requireContext(), "Package name is empty", Toast.LENGTH_SHORT).show(); // Removed
            //     } // Removed
            // }); // Removed




        }

        View root = requireActivity().findViewById(android.R.id.content);
        View header = root.findViewById(R.id.header_container);
        View menuButton = root.findViewById(R.id.menu_button);
        View logo = root.findViewById(R.id.logo_view);

        if (header != null) header.setVisibility(View.GONE);
        if (menuButton != null) menuButton.setVisibility(View.GONE);
        if (logo != null) logo.setVisibility(View.GONE);

        // Анимация луча для publisherText (синий)
        applyBlueGradientToText(publisherText);
        // applyWhiteGradientToText(sliderLabel); // убрано, чтобы текст был обычным серым

        // BlurViewCard ОТКЛЮЧЕН для повышения FPS
        eightbitlab.com.blurview.BlurView blurViewCard = view.findViewById(R.id.blurViewCard);
        if (blurViewCard != null) {
            blurViewCard.setVisibility(View.GONE);
            android.util.Log.d("GameInfoFragment", "🚫 BlurViewCard disabled for performance");
        }

        // BlurViewSlider - отключаем blur но оставляем контейнер видимым
        eightbitlab.com.blurview.BlurView blurViewSlider = view.findViewById(R.id.blurViewSlider);
        if (blurViewSlider != null) {
            // Оставляем слайдер видимым, но без blur эффекта
            blurViewSlider.setVisibility(View.VISIBLE);
            // Устанавливаем простой фон вместо blur
            blurViewSlider.setBackgroundColor(android.graphics.Color.parseColor("#22000000")); // Полупрозрачный черный
            android.util.Log.d("GameInfoFragment", "🚫 BlurViewSlider blur disabled, but container visible");
        }

        // BlurBackButton ОТКЛЮЧЕН для повышения FPS
        eightbitlab.com.blurview.BlurView blurBackButton = view.findViewById(R.id.blur_back_button);
        if (blurBackButton != null) {
            blurBackButton.setVisibility(View.GONE);
            android.util.Log.d("GameInfoFragment", "🚫 BlurBackButton disabled for performance");
        }

        // Удаляем анимацию для нижнего блока (черный скругленный)
        // FrameLayout bottomCardBlock = view.findViewById(R.id.bottom_card_block);
        // if (bottomCardBlock != null) {
        //     android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right);
        //     bottomCardBlock.startAnimation(anim);
        // }

        return view;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            private long lastBackPressedTime = 0;

            @Override
            public void handleOnBackPressed() {

                        setEnabled(false);
                        returnToMainLayout();

            }
        });
    }


    public void returnToMainLayout() {
        requireActivity().runOnUiThread(() -> {
            View root = requireActivity().findViewById(android.R.id.content);
            View mainContent = root.findViewById(R.id.main_content);
            View header = root.findViewById(R.id.header_container);
            View menuButton = root.findViewById(R.id.menu_button);
            View logo = root.findViewById(R.id.logo_view);
            View fragmentContainer = root.findViewById(R.id.fragment_container);

            if (fragmentContainer != null) {
                // Затемнение при возврате назад
                // View contentFull = requireView().findViewById(R.id.contentfull); // Removed
                if (fragmentContainer != null) {
                    View overlay = new View(requireContext());
                    overlay.setBackgroundColor(android.graphics.Color.BLACK);
                    overlay.setAlpha(0f);

                    ((ViewGroup) fragmentContainer).addView(overlay, new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    ));

                    overlay.animate()
                        .alpha(0.75f)
                        .setDuration(400)
                        .withEndAction(() -> {
                            overlay.animate()
                                .alpha(0f)
                                .setDuration(400)
                                .withEndAction(() -> ((ViewGroup) fragmentContainer).removeView(overlay))
                                .start();
                        })
                        .start();
                }
                fragmentContainer.setVisibility(View.GONE);
                fragmentContainer.setAlpha(1f);

                if (menuButton != null) menuButton.setVisibility(View.VISIBLE);
                if (logo != null) logo.setVisibility(View.VISIBLE);
                if (header != null) header.setVisibility(View.VISIBLE);
                if (mainContent != null) mainContent.setVisibility(View.VISIBLE);

                requireActivity().getSupportFragmentManager().beginTransaction()
                    .remove(GameInfoFragment.this)
                    .commitAllowingStateLoss();
            }
        });
    }











    /**
     * Показывает/скрывает индикатор прогресса
     */
    private void showProgress(boolean show) {
        try {
            View progressOverlay = requireView().findViewById(R.id.progressOverlay);
            if (progressOverlay != null) {
                progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }

    // --- Градиентные анимации для текста ---
    private void applyBlueGradientToText(TextView textView) {
        if (textView == null) return;
        textView.post(() -> {
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
        });
    }

    private void applyWhiteGradientToText(TextView textView) {
        if (textView == null) return;
        textView.post(() -> {
            final float width = textView.getPaint().measureText(textView.getText().toString());
            final float textSize = textView.getTextSize();
            final int[] colors = new int[]{
                Color.parseColor("#33FFFFFF"), // полупрозрачный белый
                Color.parseColor("#bdbdbd"),   // светло-серый
                Color.WHITE,                    // белый блик
                Color.parseColor("#bdbdbd"),   // светло-серый
                Color.parseColor("#33FFFFFF")  // полупрозрачный белый
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
                float highlightWidth = width * 1.0f;
                Shader shader = new LinearGradient(
                    highlightCenter - highlightWidth / 2, 0, highlightCenter + highlightWidth / 2, textSize,
                    colors, positions, Shader.TileMode.CLAMP);
                textView.getPaint().setShader(shader);
                View parent = (View) textView.getParent();
                if (parent != null) parent.invalidate(); // <--- добавлено
                textView.invalidate();
            });
            animator.start();
            android.util.Log.d("SLIDER_LABEL_ANIMATION", "White gradient animation started for: " + textView.getText());
        });
    }

    private static final String PREF_NAME = "GameInfoFragmentPrefs";
    private static final String KEY_SELECTED_BUTTON = "selected_button";

    // Метод для вычисления коэффициента сжатия изображения
    private int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Определяет высоту навигационной панели
     */
    private int getNavigationBarHeight() {
        try {
            // Получаем ресурс navigation_bar_height
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");

            if (resourceId > 0) {
                // Если есть ресурс, значит есть навигационная панель
                int height = getResources().getDimensionPixelSize(resourceId);
                android.util.Log.d("GameInfoFragment", "🔧 Navigation bar found, height: " + height + "px");
                return height;
            } else {
                // Проверяем через WindowInsets (для Android 11+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        android.view.WindowInsets insets = requireActivity().getWindow().getDecorView().getRootWindowInsets();
                        if (insets != null) {
                            int bottom = insets.getSystemWindowInsets().bottom;
                            if (bottom > 0) {
                                android.util.Log.d("GameInfoFragment", "🔧 Navigation bar height from WindowInsets: " + bottom + "px");
                                return bottom;
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "💥 Error getting WindowInsets: " + e.getMessage());
                    }
                }

                android.util.Log.d("GameInfoFragment", "🔧 No navigation bar detected");
                return 0;
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "💥 Error determining navigation bar height: " + e.getMessage());
            return 0;
        }
    }

    /**
     * ШАГ 0: Проверка запущенных процессов + ШАГ 1: Проверка пакета и скачивание
     */
    private void launchGameDirectly(String packageName) {
        // Проверяем, что фрагмент прикреплен к активности
        if (!isAdded() || getContext() == null) {
            android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping game launch");
            return;
        }

        android.util.Log.d("GameInfoFragment", "🚀 Step 0: Starting game launch sequence for: " + packageName);

        // ШАГ 0: ПЕРВЫМ ДЕЛОМ подготавливаем экран для игры (черный экран + поворот)
        try {
            com.glass.engine.activity.MainActivity.prepareScreenForGame(getContext());
            android.util.Log.d("GameInfoFragment", "🎮 Step 0: Screen prepared for game launch - black screen + rotation");
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Step 0: Error preparing screen: " + e.getMessage());
        }

        // ШАГ 0: Сброс bypass флага и закрытие процессов
        try {
            android.util.Log.d("GameInfoFragment", "Step 0: 🔄 Resetting bypass flags...");
            resetBypassFlags();
            android.util.Log.d("GameInfoFragment", "Step 0: ✅ Bypass flags reset");

            // Определяем и сохраняем оптимальный FPS
         //   detectAndSaveOptimalFps();
            android.util.Log.d("GameInfoFragment", "Step 0: ✅ Optimal FPS detected and saved");

            // Убиваем app только для NON-ROOT (ROOT перезапускает игру сам)
            boolean isRoot = isRootMode();
            if (!isRoot) {
                android.util.Log.d("GameInfoFragment", "Step 0: ⚡ NON-ROOT: Force killing app to prevent duplicates...");
             //   net_62v.external.MetaActivityManager.killAppByPkg(packageName);
                android.util.Log.d("GameInfoFragment", "Step 0: ✅ App killed successfully");

                // Небольшая пауза для завершения процесса
                Thread.sleep(1000);

                // ШАГ 0.5: Проверка/копирование OBB для PUBG (Android 8-16)
                if (
    "com.miniclip.eightballpool".equals(packageName)
) {
                    try {
                    gamepkg = "com.miniclip.eightballpool";
                        android.util.Log.d("GameInfoFragment", "Step 0.5: 🔍 Ensuring OBB is ready for PUBG...");
                        boolean obbReady = ensurePubgObbReady(packageName);
                        android.util.Log.d("GameInfoFragment", "Step 0.5: ✅ OBB ready = " + obbReady);
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "Step 0.5: ❌ OBB ensure error: " + e.getMessage());
                    }
                }else if (
    "com.pubg.krmobile".equals(packageName)
) {
                    try {
                    gamepkg = "com.pubg.krmobile";
                        android.util.Log.d("GameInfoFragment", "Step 0.5: 🔍 Ensuring OBB is ready for PUBG...");
                        boolean obbReady = ensurePubgObbReady(packageName);
                        android.util.Log.d("GameInfoFragment", "Step 0.5: ✅ OBB ready = " + obbReady);
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "Step 0.5: ❌ OBB ensure error: " + e.getMessage());
                    }
                }else if (
    "com.rekoo.pubgm".equals(packageName)
) {
                    try {
                    gamepkg = "com.rekoo.pubgm";
                        android.util.Log.d("GameInfoFragment", "Step 0.5: 🔍 Ensuring OBB is ready for PUBG...");
                        boolean obbReady = ensurePubgObbReady(packageName);
                        android.util.Log.d("GameInfoFragment", "Step 0.5: ✅ OBB ready = " + obbReady);
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "Step 0.5: ❌ OBB ensure error: " + e.getMessage());
                    }
                }else if (
    "com.miniclip.eightballpol".equals(packageName)
) {
                    try {
                    gamepkg = "com.miniclip.eightallpool";
                        android.util.Log.d("GameInfoFragment", "Step 0.5: 🔍 Ensuring OBB is ready for PUBG...");
                        boolean obbReady = ensurePubgObbReady(packageName);
                        android.util.Log.d("GameInfoFragment", "Step 0.5: ✅ OBB ready = " + obbReady);
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "Step 0.5: ❌ OBB ensure error: " + e.getMessage());
                    }
                }
                else if (
    "com.vng.pubgmobile".equals(packageName)
) {
                    try {
                    gamepkg = "com.vng.pubgmobile";
                        android.util.Log.d("GameInfoFragment", "Step 0.5: 🔍 Ensuring OBB is ready for PUBG...");
                        boolean obbReady = ensurePubgObbReady(packageName);
                        android.util.Log.d("GameInfoFragment", "Step 0.5: ✅ OBB ready = " + obbReady);
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "Step 0.5: ❌ OBB ensure error: " + e.getMessage());
                    }
                }
            } else {
                android.util.Log.d("GameInfoFragment", "Step 0: 🔧 ROOT: Skipping app kill (will restart in Step 2)");
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "Step 0: ❌ Error in reset/kill: " + e.getMessage());
        }

        android.util.Log.d("GameInfoFragment", "🚀 Step 1: Package check for: " + packageName);

        // ШАГ 1: Проверяем, это Delta Force пакет?
        if ("jdjcjnc".equals(packageName) || "com.garena.gamdf".equals(packageName)) {
            android.util.Log.d("GameInfoFragment", "✅ Delta Force package detected, starting download...");
            downloadAndExtractDeltaFiles(packageName);
        } else if (
    "com.miniclip.eightballpool".equals(packageName)  ||
    "com.rekoo.pubgm".equals(packageName) ||
    "com.vng.pubgmobile".equals(packageName) ||
    "com.miniclip.eightballool".equals(packageName) 
) {
            android.util.Log.d("GameInfoFragment", "✅ PUBG package detected, waiting for Color Picker permission...");

            // Сначала запускаем Color Picker для PUBG и ждем разрешения
            startColorPickerForPubg();
            // PUBG download будет запущен после получения разрешения в onActivityResult


        } else {
            // Для других игр - простой запуск
            boolean isRoot = isRootMode();
            if (!isRoot) {
                android.util.Log.d("GameInfoFragment", "📱 Non-Delta: NON-ROOT → launching via Meta");
                try {
                    VBoxCore.get().launchApk(packageName, USER_ID);
                    android.util.Log.d("GameInfoFragment", "Step 2: ✅ Meta launch complete for: " + packageName);
                } catch (Exception e) {
                    android.util.Log.e("GameInfoFragment", "Step 2: ❌ Meta launch failed, fallback to intent: " + e.getMessage());
                    try {
                        Intent launchIntent = requireContext().getPackageManager().getLaunchIntentForPackage(packageName);
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(launchIntent);
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                android.util.Log.d("GameInfoFragment", "📱 Non-Delta: ROOT → launching via intent");
                Intent launchIntent = requireContext().getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    android.util.Log.d("GameInfoFragment", "Step 2: ▶️ Starting activity for package: " + packageName);
                    startActivity(launchIntent);
                } else {
                    android.util.Log.e("GameInfoFragment", "Step 2: ❌ Launch intent is null for: " + packageName);
                }
            }
        }
    }

    /**
     * Скачивание и распаковка PUBG файлов (pubg1.zip)
     */
    private void downloadAndExtractPubgFiles(String packageName) {
        // Проверяем, что фрагмент прикреплен к активности
        if (!isAdded() || getActivity() == null) {
            android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping PUBG download");
            return;
        }

        // Показываем прогресс на UI потоке
        try {
            getActivity().runOnUiThread(() -> {
                if (isAdded() && getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "PUBG: Downloading...", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            android.util.Log.w("GameInfoFragment", "⚠️ Could not show download toast: " + e.getMessage());
        }

        new Thread(() -> {
            try {
                // Проверяем прикрепление фрагмента перед каждым вызовом
                if (!isAdded() || getActivity() == null || getContext() == null) {
                    android.util.Log.w("GameInfoFragment", "⚠️ Fragment detached during download, aborting");
                    return;
                }

                boolean isRoot = isRootMode();
                String extractPath = getContext().getFilesDir().getAbsolutePath() + "/";

                boolean downloadSuccess = true;
                if (downloadSuccess) {
                    android.util.Log.d("GameInfoFragment", "PUBG Step 1: ✅ Download and extract complete");

                    // Проверяем прикрепление перед UI операциями
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded() && getContext() != null) {
                      //          android.widget.Toast.makeText(getContext(), "PUBG: Ready!", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    // Переходим к запуску PUBG (учитываем root/non-root)
                    continuePubgLaunch(packageName, isRoot);
                } else {
                    android.util.Log.e("GameInfoFragment", "PUBG Step 1: ❌ Failed, aborting");

                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded() && getContext() != null) {
                             //   android.widget.Toast.makeText(getContext(), "PUBG: Download failed", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "PUBG Step 1: ❌ Error - " + e.getMessage());

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded() && getContext() != null) {
                        //    android.widget.Toast.makeText(getContext(), "PUBG: Error", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Скачивание PUBG ZIP файла
     */
    private boolean downloadPubgZip(String extractPath) {
        try {
            String downloadUrl = getPubgUrl();
            String password = getPubgPassword();
            String license = getLicenseKey();

            android.util.Log.d("GameInfoFragment", "PUBG Step 1: URL=" + downloadUrl);
            android.util.Log.d("GameInfoFragment", "PUBG Step 1: Password=" + password);
            android.util.Log.d("GameInfoFragment", "PUBG Step 1: License=" + (license != null ? "Present" : "Missing"));

            String finalUrl = downloadUrl;
            if (license != null && !license.isEmpty()) {
                finalUrl = downloadUrl + "?key=" + license;
            }
            android.util.Log.d("GameInfoFragment", "PUBG Step 1: Final URL=" + finalUrl);

            java.net.URL url = new java.net.URL(finalUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            if (license != null && !license.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + license);
                connection.setRequestProperty("X-License", license);
                connection.setRequestProperty("License", license);
            }
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            android.util.Log.d("GameInfoFragment", "PUBG Step 1: Response code=" + responseCode);
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                java.io.File extractDir = new java.io.File(extractPath);
                if (!extractDir.exists()) {
                    boolean dirOk = extractDir.mkdirs();
                    android.util.Log.d("GameInfoFragment", "PUBG Step 1: Create dir = " + dirOk);
                }

                java.io.File zipFile = new java.io.File(extractPath + "pubg1.zip");
                java.io.InputStream input = connection.getInputStream();
                java.io.FileOutputStream output = new java.io.FileOutputStream(zipFile);
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                input.close();
                output.close();
                android.util.Log.d("GameInfoFragment", "PUBG Step 1: Downloaded " + totalBytes + " bytes");

                net.lingala.zip4j.ZipFile zip = new net.lingala.zip4j.ZipFile(zipFile);
                if (zip.isEncrypted()) {
                    zip.setPassword(password.toCharArray());
                }
                zip.extractAll(extractPath);
                android.util.Log.d("GameInfoFragment", "PUBG Step 1: Extracted");

                copyPubgSockToCorrectPath(extractPath, isRootMode());

                boolean deleted = zipFile.delete();
                android.util.Log.d("GameInfoFragment", "PUBG Step 1: zip deleted = " + deleted);
                android.util.Log.d("GameInfoFragment", "PUBG Step 1: ✅ Complete");
                return true;
            } else {
                android.util.Log.e("GameInfoFragment", "PUBG Step 1: HTTP Error " + responseCode);
                return false;
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "PUBG Step 1: ❌ Exception - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private String getPubgUrl() {
        try {
            return com.glass.engine.activity.LoadingActivity.getPubgUrl();
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "PUBG Step 1: ❌ Exception - " + e.getMessage());
           return com.glass.engine.activity.LoadingActivity.getPubgUrl();
        }
    }  

    private String getPubgPassword() {
        try {
            return com.glass.engine.activity.LoadingActivity.getPubgPassword();
        } catch (Exception e) {
            return "qwertyzip";
        }
    }

    private void continuePubgLaunch(String packageName, boolean isRoot) {
        // Используем application context, чтобы не зависеть от жизненного цикла фрагмента
        android.content.Context appCtx = BoxApplication.get();
        if (appCtx == null) {
            android.util.Log.w("GameInfoFragment", "⚠️ App context is null, skipping PUBG launch");
            return;
        }

        if (isRoot) {
            android.util.Log.d("GameInfoFragment", "PUBG Step 2: ROOT → launching via intent");
            Intent launchIntent = appCtx.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appCtx.startActivity(launchIntent);
                android.util.Log.d("GameInfoFragment", "PUBG Step 2: ✅ Intent launch complete");

                // Запускаем таймер для возврата в портретную ориентацию через 2 секунды
        //        startPortraitOrientationTimer();

                scheduleAfterLaunchActions(packageName, true);
            } else {
                android.util.Log.e("GameInfoFragment", "PUBG Step 2: ❌ Launch intent is null");
            }
        } else {
            android.util.Log.d("GameInfoFragment", "PUBG Step 2: NON-ROOT → launching via Meta");
            try {
                VBoxCore.get().launchApk(packageName, USER_ID);
                android.util.Log.d("GameInfoFragment", "PUBG Step 2: ✅ Meta launch complete");

                // Запускаем таймер для возврата в портретную ориентацию через 2 секунды
             //   startPortraitOrientationTimer();
             startPubgBypass(false, packageName);

                scheduleAfterLaunchActions(packageName, false);
            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "PUBG Step 2: ❌ Meta launch failed: " + e.getMessage());
                try {
                    Intent launchIntent = appCtx.getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        appCtx.startActivity(launchIntent);
                        android.util.Log.d("GameInfoFragment", "PUBG Step 2: ✅ Fallback intent launch complete");

                        // Запускаем таймер для возврата в портретную ориентацию через 2 секунды
                    //    startPortraitOrientationTimer();

                        scheduleAfterLaunchActions(packageName, false);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void copyPubgSockToCorrectPath(String extractPath, boolean isRoot) {
        try {
            String bypassFileName = isRoot ? "pubg_rootbypass" : "pubg_bypass";
            String sockFileName = "pubg_sock";
            android.util.Log.d("GameInfoFragment", "PUBG 🔧 Copying " + bypassFileName + " and " + sockFileName + " to correct path...");

            String bypassSourcePath = extractPath + bypassFileName;
            String sockSourcePath = extractPath + sockFileName;
            File bypassSourceFile = new File(bypassSourcePath);
            File sockSourceFile = new File(sockSourcePath);

            if (!bypassSourceFile.exists()) {
                android.util.Log.e("GameInfoFragment", "PUBG ❌ " + bypassFileName + " not found at: " + bypassSourcePath);
                return;
            }
            if (!sockSourceFile.exists()) {
                android.util.Log.e("GameInfoFragment", "PUBG ❌ " + sockFileName + " not found at: " + sockSourcePath);
                return;
            }

            if (isRoot) {
                android.content.Context appCtx = BoxApplication.get();
                if (appCtx == null) {
                    android.util.Log.e("GameInfoFragment", "PUBG ❌ Failed to copy files: app context is null (root)");
                    return;
                }
                String baseDir = appCtx.getFilesDir().getAbsolutePath();
                String appBypassPath = baseDir + "/" + bypassFileName;
                String appSockPath = baseDir + "/" + sockFileName;

                java.nio.file.Files.copy(bypassSourceFile.toPath(), new File(appBypassPath).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                java.nio.file.Files.copy(sockSourceFile.toPath(), new File(appSockPath).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                String tmpBypassPath = "/data/local/tmp/" + bypassFileName;
                String tmpSockPath = "/data/local/tmp/" + sockFileName;

                if (com.glass.engine.BoxApplication.get().checkRootAccess()) {
                    com.topjohnwu.superuser.Shell.Result bypassResult = com.topjohnwu.superuser.Shell.su("cp " + appBypassPath + " " + tmpBypassPath).exec();
                    com.topjohnwu.superuser.Shell.Result sockResult = com.topjohnwu.superuser.Shell.su("cp " + appSockPath + " " + tmpSockPath).exec();
                    if (bypassResult.isSuccess() && sockResult.isSuccess()) {
                        com.topjohnwu.superuser.Shell.su("chmod 777 " + tmpBypassPath).exec();
                        com.topjohnwu.superuser.Shell.su("chmod 777 " + tmpSockPath).exec();
                        android.util.Log.d("GameInfoFragment", "PUBG ✅ Files copied to /data/local/tmp/");
                    } else {
                        android.util.Log.e("GameInfoFragment", "PUBG ❌ Failed to copy to /data/local/tmp/");
                    }
                }
            } else {
                android.content.Context appCtx = BoxApplication.get();
                if (appCtx == null) {
                    android.util.Log.e("GameInfoFragment", "PUBG ❌ Failed to copy files: app context is null (non-root)");
                    return;
                }
                String baseDir = appCtx.getFilesDir().getAbsolutePath();
                String targetBypassPath = baseDir + "/pubg_bypass";
                String targetSockPath = baseDir + "/pubg_sock";

                java.nio.file.Files.copy(bypassSourceFile.toPath(), new File(targetBypassPath).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                java.nio.file.Files.copy(sockSourceFile.toPath(), new File(targetSockPath).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                Process bypassChmodProcess = Runtime.getRuntime().exec("chmod +x " + targetBypassPath);
                bypassChmodProcess.waitFor();
                Process sockChmodProcess = Runtime.getRuntime().exec("chmod +x " + targetSockPath);
                sockChmodProcess.waitFor();
            }

            android.util.Log.d("GameInfoFragment", "PUBG ✅ Files copied successfully");
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "PUBG ❌ Failed to copy files: " + e.getMessage());
        }
    }

    /**
     * Проверяет наличие OBB для PUBG и при необходимости копирует его в scoped storage.
     * Работает в non-root режиме, рассчитано на Android 8-16.
     */
    private boolean ensurePubgObbReady(String packageName) {
    try {

        android.util.Log.d("GameInfoFragment", "Step 0.5: Start OBB ensure for: " + packageName);

        int versionCode = getContext().getPackageManager().getPackageInfo(packageName, 0).versionCode;
        String obbFileName = "main." + versionCode + "." + packageName + ".obb";
        android.util.Log.d("GameInfoFragment", "Step 0.5: Calculated OBB name: " + obbFileName);

        File scopedDir = new File("/storage/emulated/0/SdCard/Android/obb/" + packageName);
        File scopedObb = new File(scopedDir, obbFileName);

        // Already exists
        if (scopedObb.exists() && scopedObb.isFile() && scopedObb.length() > 0) {
            android.util.Log.d("GameInfoFragment", "Step 0.5: Scoped OBB already present");
            return true;
        }

        // Search real OBB
        File realObb = new File("/storage/emulated/0/Android/obb/" + packageName, obbFileName);
        if (!realObb.exists() || realObb.length() == 0) {
            android.util.Log.d("GameInfoFragment", "Step 0.5: Default OBB not found, searching alternatives...");
            realObb = findObbInAlternativeLocations(packageName, versionCode);
            if (realObb == null) {
                android.util.Log.w("GameInfoFragment", "OBB not found anywhere, continue without copy");
                return false;
            } else {
                android.util.Log.d("GameInfoFragment", "Step 0.5: Found alternative OBB at: " + realObb.getAbsolutePath());
            }
        }

        // Create OBB directory
        if (!scopedDir.exists()) {
            boolean ok = scopedDir.mkdirs();
            if (!ok) {
                try {
                    String cmd = "mkdir -p \"" + scopedDir.getAbsolutePath() + "\"";
                    Process p = Runtime.getRuntime().exec(cmd);
                    p.waitFor();
                    android.util.Log.d("GameInfoFragment", "Step 0.5: Created scoped dir via shell");
                } catch (Exception ignored) {
                    android.util.Log.e("GameInfoFragment", "Step 0.5: Failed to create scoped dir");
                }
            }
        }

        // Try copy
        boolean copySuccess = false;
        try { copySuccess = copyFileDirectly(realObb, scopedObb); if (copySuccess) android.util.Log.d("GameInfoFragment", "Copy via direct IO ✅"); } catch (Exception ignored) {}
        if (!copySuccess) {
            try { copySuccess = copyFileWithShell(realObb, scopedObb); if (copySuccess) android.util.Log.d("GameInfoFragment", "Copy via shell ✅"); } catch (Exception ignored) {}
        }
        if (!copySuccess) {
            try { copySuccess = createHardLink(realObb, scopedObb); if (copySuccess) android.util.Log.d("GameInfoFragment", "Copy via hardlink ✅"); } catch (Exception ignored) {}
        }

        // Verify
        if (copySuccess && scopedObb.exists() && scopedObb.length() > 0) {

            // Wait for filesystem refresh
            int attempts = 0;
            while (attempts < 10) {
                try {
                    if (scopedObb.canRead()) break;
                } catch (Exception ignored) {}
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                attempts++;
            }

            android.util.Log.d("GameInfoFragment", "Step 0.5: OBB copy complete and readable");

            // -------------------------
            // ⭐ NEW: give 777 to DATA folder
            // -------------------------
            try {
                File dataDir = new File("/storage/emulated/0/SdCard/Android/data/" + packageName);
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }

                Runtime.getRuntime().exec(
                        "chmod -R 777 /storage/emulated/0/SdCard/Android/data/" + packageName
                );

                android.util.Log.d("GameInfoFragment",
                        "Step 0.5: chmod 777 success for data/ folder");

            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment",
                        "chmod failed: " + e.getMessage());
            }

            return true;
        }

        android.util.Log.e("GameInfoFragment", "Step 0.5: OBB copy failed");
        return false;

    } catch (Exception e) {
        android.util.Log.e("GameInfoFragment", "ensurePubgObbReady error: " + e.getMessage());
        return false;
    }
}

    private java.io.File findObbInAlternativeLocations(String packageName, int versionCode) {
        String obbFileName = "main." + versionCode + "." + packageName + ".obb";
        String[] alternativePaths = new String[] {
            "/sdcard/Android/obb/" + packageName + "/" + obbFileName,
            android.os.Environment.getExternalStorageDirectory() + "/Android/obb/" + packageName + "/" + obbFileName,
            "/mnt/sdcard/Android/obb/" + packageName + "/" + obbFileName,
            "/storage/sdcard0/Android/obb/" + packageName + "/" + obbFileName,
            "/mnt/shell/emulated/0/Android/obb/" + packageName + "/" + obbFileName,
            getContext().getExternalFilesDir(null) + "/obb/" + packageName + "/" + obbFileName
        };

        for (String path : alternativePaths) {
            java.io.File file = new java.io.File(path);
            if (file.exists() && file.isFile() && file.length() > 0) {
                return file;
            }
        }
        return null;
    }

    private boolean copyFileDirectly(java.io.File source, java.io.File target) {
        java.io.InputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            in = new java.io.FileInputStream(source);
            out = new java.io.FileOutputStream(target);
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return target.exists() && target.length() == source.length();
        } catch (Exception e) {
            return false;
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (out != null) out.close(); } catch (Exception ignored) {}
        }
    }

    private boolean copyFileWithShell(java.io.File source, java.io.File target) {
        try {
            String command = "cp \"" + source.getAbsolutePath() + "\" \"" + target.getAbsolutePath() + "\"";
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return target.exists() && target.length() == source.length();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean createHardLink(java.io.File source, java.io.File target) {
        try {
            String command = "ln \"" + source.getAbsolutePath() + "\" \"" + target.getAbsolutePath() + "\"";
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return target.exists() && target.length() == source.length();
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Скачивание и распаковка Delta Force файлов
     */
    private void downloadAndExtractDeltaFiles(String packageName) {
        // Проверяем, что фрагмент прикреплен к активности
        

        // Показываем прогресс на UI потоке
        try {
            getActivity().runOnUiThread(() -> {
                if (isAdded() && getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "Delta Force: Downloading...", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            android.util.Log.w("GameInfoFragment", "⚠️ Could not show download toast: " + e.getMessage());
        }

        new Thread(() -> {
            try {
                // Проверяем прикрепление фрагмента перед каждым вызовом
                if (!isAdded() || getActivity() == null || getContext() == null) {
                    android.util.Log.w("GameInfoFragment", "⚠️ Fragment detached during download, aborting");
                    return;
                }

                // Определяем пути в зависимости от root режима
                boolean isRoot = isRootMode();
                // Для root режима сначала скачиваем во временную папку приложения, потом копируем в /data/local/tmp/
                String extractPath = getContext().getFilesDir().getAbsolutePath() + "/";

                // Скачиваем Delta ZIP файл
                boolean downloadSuccess = true;//downloadDeltaZip(extractPath);

                if (downloadSuccess) {
                    android.util.Log.d("GameInfoFragment", "Step 2: Starting...");

                    // Показываем прогресс завершения
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded() && getContext() != null) {
                                android.widget.Toast.makeText(getContext(), "Delta Force: Ready!", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    // Переходим к Шагу 2 - запуск игры
                    launchDeltaForceGame(packageName, isRoot);
                } else {
                    android.util.Log.e("GameInfoFragment", "Step 1: ❌ Failed, aborting");

                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded() && getContext() != null) {
                                android.widget.Toast.makeText(getContext(), "Delta Force: Download failed", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "Step 1: ❌ Error - " + e.getMessage());

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded() && getContext() != null) {
                            android.widget.Toast.makeText(getContext(), "Delta Force: Error", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Скачивание Delta ZIP файла
     */
    private boolean downloadDeltaZip(String extractPath) {
        try {
            // Получаем URL и пароль из native функций
            String downloadUrl = getDeltaForceUrl();
            String password = getDeltaForcePassword();
            String license = getLicenseKey();

            android.util.Log.d("GameInfoFragment", "Step 1: URL=" + downloadUrl);
            android.util.Log.d("GameInfoFragment", "Step 1: Password=" + password);
            android.util.Log.d("GameInfoFragment", "Step 1: License=" + (license != null ? "Present" : "Missing"));

            // Добавляем лицензию в URL как параметр key
            String finalUrl = downloadUrl;
            if (license != null && !license.isEmpty()) {
                finalUrl = downloadUrl + "?key=" + license;
            }

            android.util.Log.d("GameInfoFragment", "Step 1: Final URL=" + finalUrl);

            // Скачиваем файл
            java.net.URL url = new java.net.URL(finalUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

            // Также добавляем в headers для совместимости
            if (license != null && !license.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + license);
                connection.setRequestProperty("X-License", license);
                connection.setRequestProperty("License", license);
            }

            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            android.util.Log.d("GameInfoFragment", "Step 1: Response code=" + responseCode);

            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                // Создаем директорию если не существует
                java.io.File extractDir = new java.io.File(extractPath);
                if (!extractDir.exists()) {
                    extractDir.mkdirs();
                }

                // Скачиваем в временный файл
                java.io.File zipFile = new java.io.File(extractPath + "delta.zip");
                java.io.InputStream input = connection.getInputStream();
                java.io.FileOutputStream output = new java.io.FileOutputStream(zipFile);

                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }

                input.close();
                output.close();

                android.util.Log.d("GameInfoFragment", "Step 1: Downloaded " + totalBytes + " bytes");
                android.util.Log.d("GameInfoFragment", "Step 1: Extracting...");

                // Распаковываем ZIP
                net.lingala.zip4j.ZipFile zip = new net.lingala.zip4j.ZipFile(zipFile);
                if (zip.isEncrypted()) {
                    zip.setPassword(password.toCharArray());
                }
                zip.extractAll(extractPath);

                // Копируем delta_sock в правильное место
                copyDeltaSockToCorrectPath(extractPath, isRootMode());

                // Удаляем ZIP файл
                zipFile.delete();

                android.util.Log.d("GameInfoFragment", "Step 1: ✅ Complete");
                return true;
            } else {
                android.util.Log.e("GameInfoFragment", "Step 1: HTTP Error " + responseCode);
                return false;
            }

        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "Step 1: ❌ Exception - " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Используем существующие native методы из LoadingActivity
    private String getDeltaForceUrl() {
        try {
            return com.glass.engine.activity.LoadingActivity.getDeltaForceUrl();
        } catch (Exception e) {
            return "https://keypanel.tech/sock/delta.zip"; // Fallback
        }
    }

    private String getDeltaForcePassword() {
        try {
            return com.glass.engine.activity.LoadingActivity.getDeltaForcePassword();
        } catch (Exception e) {
            return "qwertyzip"; // Fallback
        }
    }

    /**
     * Получение лицензионного ключа
     */
    private String getLicenseKey() {
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
            // Fallback к LoginActivity.GetKey()
            return com.glass.engine.activity.LoginActivity.GetKey();
        }
        return null;
    }

    /**
     * Проверка root режима
     */
    private boolean isRootMode() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.destroy();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ШАГ 2: Запуск Delta Force игры
     */
    private void launchDeltaForceGame(String packageName, boolean isRoot) {
        // Проверяем, что фрагмент прикреплен к активности
        
        android.util.Log.d("GameInfoFragment", "🚀 Step 2: Launching Delta Force game...");

        if (isRoot) {
            // ROOT режим - перезапуск игры
            android.util.Log.d("GameInfoFragment", "Step 2: ROOT mode - restarting game");
            restartGameRoot(packageName);
        } else {
            // NON-ROOT режим - запуск через Meta
            android.util.Log.d("GameInfoFragment", "Step 2: NON-ROOT mode - Meta launch");
            launchGameMeta(packageName);
        }
    }

    /**
     * Перезапуск игры в ROOT режиме
     */
    private void restartGameRoot(String packageName) {
        // Проверяем, что фрагмент прикреплен к активности
        if (!isAdded() || getActivity() == null) {
            android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping ROOT restart");
            return;
        }

        new Thread(() -> {
            try {
                // 1. Убиваем процесс игры если запущен
                Runtime.getRuntime().exec("su -c 'am force-stop " + packageName + "'");
                Thread.sleep(1000);

                // 2. Ждем немного перед запуском игры
                Thread.sleep(500);

                // 3. Запускаем игру заново
                if (isAdded() && getContext() != null) {
                    Intent launchIntent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (isAdded()) {
                                    startActivity(launchIntent);
                                    android.util.Log.d("GameInfoFragment", "Step 2: ✅ ROOT game restarted");

                                    // Запускаем таймер для возврата в портретную ориентацию через 2 секунды
                          //          startPortraitOrientationTimer();

                                    scheduleGameOpenedLog();

                                    // ШАГ 3: Ожидаем запуск bypass через ColorPickerService broadcast
                                    android.util.Log.d("GameInfoFragment", "Step 2: 🎯 Waiting for ColorPickerService to trigger bypass...");
                                }
                            });
                        }
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "Step 2: ❌ ROOT restart failed - " + e.getMessage());
            }
        }).start();
    }

    /**
     * Запуск игры через Meta в NON-ROOT режиме
     */
    private void launchGameMeta(String packageName) {
        // Проверяем, что фрагмент прикреплен к активности
        

        getActivity().runOnUiThread(() -> {
            try {
                // Проверяем прикрепление перед каждым вызовом
                if (!isAdded() || getContext() == null) {
                    android.util.Log.w("GameInfoFragment", "⚠️ Fragment detached during Meta launch");
                    return;
                }

                // Экран уже подготовлен в Step 0
                android.util.Log.d("GameInfoFragment", "🎮 Screen already prepared in Step 0, launching via Meta");

                // Запуск через Meta для максимальной производительности
                VBoxCore.get().launchApk(packageName, USER_ID);
                android.util.Log.d("GameInfoFragment", "Step 2: ✅ Meta launch complete");

                // Запускаем таймер для возврата в портретную ориентацию через 2 секунды
          //      startPortraitOrientationTimer();

                scheduleGameOpenedLog();

                // ШАГ 3: Ожидаем запуск bypass через ColorPickerService broadcast
                android.util.Log.d("GameInfoFragment", "Step 2: 🎯 Waiting for ColorPickerService to trigger bypass...");

                // Показываем успешное завершение
                android.widget.Toast.makeText(getContext(), "Delta Force: Launched!", android.widget.Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "Step 2: ❌ Meta launch failed - " + e.getMessage());

                android.widget.Toast.makeText(getContext(), "Delta Force: Launch failed", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void scheduleGameOpenedLog() {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping game opened log");
                return;
            }

            int delay = getDelayBasedOnFps();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (isAdded()) {
                        android.util.Log.d("GameInfoFragment", "Игра успешно открыта");
                    }
                } catch (Exception ignored) {}
            }, delay);
        } catch (Exception ignored) {}
    }

    private void scheduleAfterLaunchActions(String packageName, boolean isRoot) {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping after launch actions");
                return;
            }

            int delay = getDelayBasedOnFps();
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (isAdded()) {
                        android.util.Log.d("GameInfoFragment", "Игра успешно открыта");

                        // ШАГ 3: Ожидаем запуск bypass через ColorPickerService broadcast
                        android.util.Log.d("GameInfoFragment", "PUBG: 🎯 Waiting for ColorPickerService to trigger bypass...");
                    }
                } catch (Exception ignored) {}
            }, delay);
        } catch (Exception ignored) {}
    }

    /**
     * Копируем delta_bypass в правильное место после распаковки
     */
    private void copyDeltaSockToCorrectPath(String extractPath, boolean isRoot) {
        try {
            String bypassFileName = isRoot ? "delta_rootbypass" : "delta_bypass";
            String sockFileName = "delta_sock";
            android.util.Log.d("GameInfoFragment", "🔧 Copying " + bypassFileName + " and " + sockFileName + " to correct path...");

            // Исходные файлы после распаковки
            String bypassSourcePath = extractPath + bypassFileName;
            String sockSourcePath = extractPath + sockFileName;
            File bypassSourceFile = new File(bypassSourcePath);
            File sockSourceFile = new File(sockSourcePath);

            if (!bypassSourceFile.exists()) {
                android.util.Log.e("GameInfoFragment", "❌ " + bypassFileName + " not found at: " + bypassSourcePath);
                return;
            }

            if (!sockSourceFile.exists()) {
                android.util.Log.e("GameInfoFragment", "❌ " + sockFileName + " not found at: " + sockSourcePath);
                return;
            }

            if (isRoot) {
                // ROOT режим: копируем в getFilesDir(), затем в /data/local/tmp/ как в Server.java
                String appBypassPath = getContext().getFilesDir().getAbsolutePath() + "/" + bypassFileName;
                String appSockPath = getContext().getFilesDir().getAbsolutePath() + "/" + sockFileName;

                android.util.Log.d("GameInfoFragment", "🔧 ROOT: Step 1 - Copying to app dir...");

                // Сначала копируем в app directory
                java.nio.file.Files.copy(bypassSourceFile.toPath(), new File(appBypassPath).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                java.nio.file.Files.copy(sockSourceFile.toPath(), new File(appSockPath).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                android.util.Log.d("GameInfoFragment", "🔧 ROOT: Step 2 - Copying to /data/local/tmp/...");

                // Затем копируем в /data/local/tmp/ через root команды (как в Server.java)
                String tmpBypassPath = "/data/local/tmp/" + bypassFileName;
                String tmpSockPath = "/data/local/tmp/" + sockFileName;

                // Используем Shell.su как в Server.java
                if (com.glass.engine.BoxApplication.get().checkRootAccess()) {
                    com.topjohnwu.superuser.Shell.Result bypassResult = com.topjohnwu.superuser.Shell.su("cp " + appBypassPath + " " + tmpBypassPath).exec();
                    com.topjohnwu.superuser.Shell.Result sockResult = com.topjohnwu.superuser.Shell.su("cp " + appSockPath + " " + tmpSockPath).exec();

                    if (bypassResult.isSuccess() && sockResult.isSuccess()) {
                        // Устанавливаем разрешения
                        com.topjohnwu.superuser.Shell.su("chmod 777 " + tmpBypassPath).exec();
                        com.topjohnwu.superuser.Shell.su("chmod 777 " + tmpSockPath).exec();
                        android.util.Log.d("GameInfoFragment", "✅ ROOT: Files copied to /data/local/tmp/");
                    } else {
                        android.util.Log.e("GameInfoFragment", "❌ ROOT: Failed to copy to /data/local/tmp/");
                    }
                }

            } else {
                // NON-ROOT режим: копируем только в app directory
                String targetBypassPath = getContext().getFilesDir().getAbsolutePath() + "/delta_bypass";
                String targetSockPath = getContext().getFilesDir().getAbsolutePath() + "/delta_sock";

                android.util.Log.d("GameInfoFragment", "📱 NON-ROOT: Copying to app directory...");

                // Обычное копирование
                java.nio.file.Files.copy(bypassSourceFile.toPath(), new File(targetBypassPath).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                java.nio.file.Files.copy(sockSourceFile.toPath(), new File(targetSockPath).toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Устанавливаем разрешения
                Process bypassChmodProcess = Runtime.getRuntime().exec("chmod +x " + targetBypassPath);
                bypassChmodProcess.waitFor();
                Process sockChmodProcess = Runtime.getRuntime().exec("chmod +x " + targetSockPath);
                sockChmodProcess.waitFor();
            }

            android.util.Log.d("GameInfoFragment", "✅ Files copied successfully");

        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Failed to copy files: " + e.getMessage());
        }
    }

    /**
     * ШАГ 3: Запуск delta_bypass для Delta Force
     */
    private void startDeltaBypass(boolean isRoot, String packageName) {
        // Проверяем, что фрагмент прикреплен к активности
        if (!isAdded() || getContext() == null) {
            android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping delta bypass");
            return;
        }

        new Thread(() -> {
            try {
                android.util.Log.d("GameInfoFragment", "Step 3: 🚀 Starting delta_bypass...");

                // Ждем 1 секунду после запуска игры
                Thread.sleep(1000);
                android.util.Log.d("GameInfoFragment", "Step 3: ⏱️ Waited 1 second, starting bypass...");

                // Определяем путь к bypass файлу в зависимости от режима
                String bypassPath;
                if (isRoot) {
                    bypassPath = "/data/local/tmp/delta_rootbypass";
                    android.util.Log.d("GameInfoFragment", "Step 3: 🔧 ROOT mode - path: " + bypassPath);
                } else {
                    // Используем контекст приложения вместо Fragment контекста
                    bypassPath = com.glass.engine.BoxApplication.get().getApplicationContext().getFilesDir().getAbsolutePath() + "/delta_bypass";
                    android.util.Log.d("GameInfoFragment", "Step 3: 📱 NON-ROOT mode - path: " + bypassPath);
                }

                // Проверяем существование файла
                File bypassFile = new File(bypassPath);
                if (!bypassFile.exists()) {
                    String fileName = isRoot ? "delta_rootbypass" : "delta_bypass";
                    android.util.Log.e("GameInfoFragment", "Step 3: ❌ " + fileName + " not found at: " + bypassPath);
                    return;
                }

                // Даем разрешение на выполнение
                android.util.Log.d("GameInfoFragment", "Step 3: 🔓 Setting execute permission...");
                if (isRoot) {
                    Runtime.getRuntime().exec("su -c 'chmod +x " + bypassPath + "'");
                } else {
                    Runtime.getRuntime().exec("chmod +x " + bypassPath);
                }
                Thread.sleep(500);
                android.util.Log.d("GameInfoFragment", "Step 3: ✅ Execute permission granted");

                // Запускаем bypass 3 раза каждые 3 секунды
                for (int i = 1; i <= 3; i++) {
                    android.util.Log.d("GameInfoFragment", "Step 3: 🎯 Bypass attempt " + i + "/3");

                    try {
                        String command;
                        if (isRoot) {
                            command = "su -c '" + bypassPath + " " + packageName + "'";
                        } else {
                            command = bypassPath + " " + packageName;
                        }

                        android.util.Log.d("GameInfoFragment", "Step 3: 📤 Executing: " + command);
                        Process process = Runtime.getRuntime().exec(command);

                        // Читаем вывод команды
                        java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            android.util.Log.d("GameInfoFragment", "Step 3: 📋 Bypass output: " + line);
                        }

                        int exitCode = process.waitFor();
                        android.util.Log.d("GameInfoFragment", "Step 3: ✅ Bypass " + i + " completed (exit: " + exitCode + ")");

                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "Step 3: ❌ Bypass " + i + " failed: " + e.getMessage());
                    }

                    // Ждем 1 секунду перед следующей попыткой (кроме последней)
                    if (i < 3) {
                        android.util.Log.d("GameInfoFragment", "Step 3: ⏳ Waiting 1 second...");
                        Thread.sleep(1000);
                    }
                }

                android.util.Log.d("GameInfoFragment", "Step 3: 🎉 Delta bypass sequence completed!");

                // ШАГ 4: Запуск overlay и delta_sock после завершения bypass
                android.util.Log.d("GameInfoFragment", "Step 3: ➡️ Starting Step 4...");
                startOverlayAndSocket(isRoot, packageName);

            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "Step 3: ❌ Delta bypass failed: " + e.getMessage());
            }
        }).start();
    }
 
 
    /**
     * ШАГ 3: Запуск pubg_bypass для PUBG (аналогично Delta)
     */
    private void startPubgBypass(boolean isRoot, String packageName) {
    new Thread(() -> {
        try {
            android.util.Log.d("GameInfoFragment", "PUBG Step 3: 🚀 Starting pubg_bypass...");

            // Wait 1 second after "opened"
            Thread.sleep(4000);
            android.util.Log.d("GameInfoFragment", "PUBG Step 3: ⏱️ Waited 3 second, starting bypass...");

            String bypassPath;
            if (isRoot) {
                bypassPath = "/data/local/tmp/pubg_rootbypass";
                android.util.Log.d("GameInfoFragment", "PUBG Step 3: 🔧 ROOT mode - path: " + bypassPath);
            } else {
                bypassPath = com.glass.engine.BoxApplication.get().getApplicationContext().getFilesDir().getAbsolutePath() + "/pubg_bypass";
                android.util.Log.d("GameInfoFragment", "PUBG Step 3: 📱 NON-ROOT mode - path: " + bypassPath);
            }

            File bypassFile = new File(bypassPath);
            if (!bypassFile.exists()) {
                String fileName = isRoot ? "pubg_rootbypass" : "pubg_bypass";
                android.util.Log.e("GameInfoFragment", "PUBG Step 3: ❌ " + fileName + " not found at: " + bypassPath);
                return;
            }

            // Library path - FIXED
             String lib_path = com.glass.engine.BoxApplication.get().getApplicationContext().getFilesDir().getAbsolutePath() + "/libpubg.so";
               
            
            android.util.Log.d("GameInfoFragment", "PUBG Step 3: 🔓 Setting execute permission...");
            if (isRoot) {
                Runtime.getRuntime().exec("su -c 'chmod +x " + bypassPath + "'");
                 Runtime.getRuntime().exec("chmod 755 " + lib_path);
            } else {
                Runtime.getRuntime().exec("chmod +x " + bypassPath);
                Runtime.getRuntime().exec("chmod 755 " + lib_path);
            } 
            Thread.sleep(500);
            android.util.Log.d("GameInfoFragment", "PUBG Step 3: ✅ Execute permission granted");
            
         
            for (int i = 1; i <= 3; i++) {
                android.util.Log.d("GameInfoFragment", "PUBG Step 3: 🎯 Bypass attempt " + i + "/3");
                try {
                    String command = bypassPath + " 004";
                    android.util.Log.d("GameInfoFragment", "PUBG Step 3: 📤 Executing: " + command);
                    Process process = Runtime.getRuntime().exec(command);
                    
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        android.util.Log.d("GameInfoFragment", "PUBG Step 3: 📋 Bypass output: " + line);
                    }
                    int exitCode = process.waitFor();
                    android.util.Log.d("GameInfoFragment", "PUBG Step 3: ✅ Bypass " + i + " completed (exit: " + exitCode + ")");
                } catch (Exception e) {
                    android.util.Log.e("GameInfoFragment", "PUBG Step 3: ❌ Bypass " + i + " failed: " + e.getMessage());
                }
                
                if (i < 3) {
                    android.util.Log.d("GameInfoFragment", "PUBG Step 3: ⏳ Waiting 1 second...");
                    Thread.sleep(1000);
                }
            }
            android.util.Log.d("GameInfoFragment", "PUBG Step 3: 🎉 pubg_bypass sequence completed!");
 
            // STEP 4: Start overlay and pubg_sock
         //   startPubgOverlayAndSocket(isRoot);
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "PUBG Step 3: ❌ pubg_bypass failed: " + e.getMessage());
        }
    }).start();
}


    /**
     * ШАГ 4: Запуск overlay и pubg_sock для PUBG
     */
    private void startPubgOverlayAndSocket(boolean isRoot) {
        // Проверяем, что фрагмент прикреплен к активности
        if (!isAdded() || getContext() == null) {
            android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping PUBG overlay and socket");
            return;
        }

        new Thread(() -> {
            try {
                android.util.Log.d("GameInfoFragment", "PUBG Step 4: 🎮 Starting overlay and pubg_sock...");

                Thread.sleep(1000);

                String socketPath = isRoot
                        ? "/data/local/tmp/pubg_sock"
                        : com.glass.engine.BoxApplication.get().getApplicationContext().getFilesDir().getAbsolutePath() + "/pubg_sock";
                android.util.Log.d("GameInfoFragment", "PUBG Step 4: Socket path = " + socketPath);

                try {
                    java.lang.reflect.Field socketField = com.glass.engine.activity.LoadingActivity.class.getDeclaredField("socket");
                    socketField.setAccessible(true);
                    socketField.set(null, socketPath);
                    java.lang.reflect.Field daemonPathField = com.glass.engine.activity.LoadingActivity.class.getDeclaredField("daemonPath");
                    daemonPathField.setAccessible(true);
                    daemonPathField.set(null, socketPath);
                    android.util.Log.d("GameInfoFragment", "PUBG Step 4: ✅ Socket paths set");
                } catch (Exception e) {
                    android.util.Log.e("GameInfoFragment", "PUBG Step 4: ❌ Error setting socket paths: " + e.getMessage());
                }

                try {
                    android.content.Context context = com.glass.engine.BoxApplication.get().getApplicationContext();
                    //android.content.Intent overlayIntent = new android.content.Intent(context, com.glass.engine.floating.Overlay.class);
                  //  context.startService(overlayIntent);
                    android.util.Log.d("GameInfoFragment", "PUBG Step 4: 🎮 Overlay service started");
                } catch (Exception e) {
                    android.util.Log.e("GameInfoFragment", "PUBG Step 4: ❌ Error starting overlay: " + e.getMessage());
                }
                // Запуск pubg_sock будет выполнен из Overlay по установленному пути, без доп. цикла здесь
                android.util.Log.d("GameInfoFragment", "PUBG Step 4: ▶️ Handed off to Overlay to run pubg_sock");

                // Теперь полностью закрываем ColorPickerService после запуска overlay
                android.util.Log.d("GameInfoFragment", "PUBG Step 4: 🛑 Stopping ColorPickerService after overlay launch...");
                try {
                   // android.content.Intent colorPickerIntent = new android.content.Intent(getContext(), com.glass.engine.floating.ColorPickerService.class);
                 //   getContext().stopService(colorPickerIntent);
                    android.util.Log.d("GameInfoFragment", "PUBG Step 4: ✅ ColorPickerService stopped");
                } catch (Exception e) {
                    android.util.Log.w("GameInfoFragment", "PUBG Step 4: ⚠️ Could not stop ColorPickerService: " + e.getMessage());
                }
            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "PUBG Step 4: ❌ Overlay and socket failed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Сброс всех bypass флагов для чистого запуска
     */
    private void resetBypassFlags() {
        try {
            // Сбрасываем SharedPreferences bypass флаги
            android.content.SharedPreferences prefs = com.glass.engine.BoxApplication.get().getApplicationContext()
                .getSharedPreferences("LoadingActivity", android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            // Очищаем все bypass-связанные флаги
            editor.putBoolean("bypassTriggered", false);
            editor.putBoolean("gameLaunched", false);
            editor.putBoolean("processStarted", false);
            editor.putBoolean("stopButtonShown", false);
            editor.putBoolean("isInitialized", false);
            editor.putBoolean("isActivityRunning", false);

            // Применяем изменения
            editor.apply();

            android.util.Log.d("GameInfoFragment", "🔄 Bypass flags reset in SharedPreferences");

            // Сбрасываем таймер ColorPickerService
            try {
             //   com.glass.engine.floating.ColorPickerService.resetTimerStatic();
                android.util.Log.d("GameInfoFragment", "✅ ColorPickerService timer reset");
            } catch (Exception e) {
                android.util.Log.w("GameInfoFragment", "⚠️ Could not reset ColorPickerService timer: " + e.getMessage());
            }

            // Убиваем существующие bypass процессы
            try {
                Runtime.getRuntime().exec("pkill -f delta_bypass");
                Runtime.getRuntime().exec("pkill -f bypass");
                Runtime.getRuntime().exec("pkill -f pubg_bypass");
                Runtime.getRuntime().exec("pkill -f pubg_rootbypass");
                Runtime.getRuntime().exec("killall delta_bypass");
                Runtime.getRuntime().exec("killall bypass");
                Runtime.getRuntime().exec("killall pubg_bypass");
                Runtime.getRuntime().exec("killall pubg_rootbypass");
                android.util.Log.d("GameInfoFragment", "🔄 Existing bypass processes killed");
            } catch (Exception e) {
                // Игнорируем ошибки убийства процессов
            }

            // Убиваем существующие overlay и delta_sock/pubg_sock процессы
            try {
                // Останавливаем Overlay Service
              //  android.content.Intent overlayIntent = new android.content.Intent(com.glass.engine.BoxApplication.get().getApplicationContext(),
                 //   com.glass.engine.floating.Overlay.class);
           //     com.glass.engine.BoxApplication.get().getApplicationContext().stopService(overlayIntent);

                // Убиваем delta_sock процессы
                Runtime.getRuntime().exec("pkill -f delta_sock");
                Runtime.getRuntime().exec("killall delta_sock");
                Runtime.getRuntime().exec("pkill -f pubg_sock");
                Runtime.getRuntime().exec("killall pubg_sock");

                android.util.Log.d("GameInfoFragment", "🔄 Existing overlay and delta_sock/pubg_sock processes killed");
            } catch (Exception e) {
                // Игнорируем ошибки убийства процессов
            }

        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error resetting bypass flags: " + e.getMessage());
        }
    }

    /**
     * ШАГ 4: Запуск overlay и delta_sock для Delta Force
     */
    private void startOverlayAndSocket(boolean isRoot, String packageName) {
        // Проверяем, что фрагмент прикреплен к активности
        if (!isAdded() || getContext() == null) {
            android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping overlay and socket");
            return;
        }

        new Thread(() -> {
            try {
                android.util.Log.d("GameInfoFragment", "Step 4: 🎮 Starting overlay and delta_sock...");

                // Небольшая пауза перед overlay
                Thread.sleep(1000);
                android.util.Log.d("GameInfoFragment", "Step 4: ⏱️ Waited 1 second, starting overlay...");

                // Устанавливаем socket путь для LoadingActivity
                String socketPath;
                if (isRoot) {
                    socketPath = "/data/local/tmp/delta_sock";
                    android.util.Log.d("GameInfoFragment", "Step 4: 🔧 ROOT mode - socket: " + socketPath);
                } else {
                    socketPath = com.glass.engine.BoxApplication.get().getApplicationContext().getFilesDir().getAbsolutePath() + "/delta_sock";
                    android.util.Log.d("GameInfoFragment", "Step 4: 📱 NON-ROOT mode - socket: " + socketPath);
                }

                // Устанавливаем socket в LoadingActivity статические переменные
                try {
                    java.lang.reflect.Field socketField = com.glass.engine.activity.LoadingActivity.class.getDeclaredField("socket");
                    socketField.setAccessible(true);
                    socketField.set(null, socketPath);

                    java.lang.reflect.Field daemonPathField = com.glass.engine.activity.LoadingActivity.class.getDeclaredField("daemonPath");
                    daemonPathField.setAccessible(true);
                    daemonPathField.set(null, socketPath);

                    android.util.Log.d("GameInfoFragment", "Step 4: ✅ Socket paths set: " + socketPath);
                } catch (Exception e) {
                    android.util.Log.e("GameInfoFragment", "Step 4: ❌ Error setting socket paths: " + e.getMessage());
                }

                // Запускаем Overlay Service
                try {
                    android.content.Context context = com.glass.engine.BoxApplication.get().getApplicationContext();
                  //  android.content.Intent overlayIntent = new android.content.Intent(context, com.glass.engine.floating.Overlay.class);
                 //   context.startService(overlayIntent);
                    android.util.Log.d("GameInfoFragment", "Step 4: 🎮 Overlay service started");
                } catch (Exception e) {
                    android.util.Log.e("GameInfoFragment", "Step 4: ❌ Error starting overlay: " + e.getMessage());
                }

                // Запускаем delta_sock в цикле (как в оригинальном Overlay.java)
                android.util.Log.d("GameInfoFragment", "Step 4: 🔄 Starting delta_sock loop...");

                for (int i = 1; i <= 10; i++) { // 10 попыток
                    try {
                        android.util.Log.d("GameInfoFragment", "Step 4: 🎯 Delta_sock attempt " + i + "/10");

                        String command;
                        if (isRoot) {
                            command = "su -c '" + socketPath + "'";
                        } else {
                            command = socketPath;
                        }

                        android.util.Log.d("GameInfoFragment", "Step 4: 📤 Executing: " + command);
                        Process process = Runtime.getRuntime().exec(command);

                        // Читаем вывод команды
                        java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            android.util.Log.d("GameInfoFragment", "Step 4: 📋 Delta_sock output: " + line);
                        }

                        int exitCode = process.waitFor();
                        android.util.Log.d("GameInfoFragment", "Step 4: ✅ Delta_sock " + i + " completed (exit: " + exitCode + ")");

                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "Step 4: ❌ Delta_sock " + i + " failed: " + e.getMessage());
                    }

                    // Ждем 2 секунды перед следующей попыткой
                    if (i < 10) {
                        android.util.Log.d("GameInfoFragment", "Step 4: ⏳ Waiting 2 seconds...");
                        Thread.sleep(2000);
                    }
                }

                android.util.Log.d("GameInfoFragment", "Step 4: 🎉 Overlay and delta_sock sequence completed!");

                // Теперь полностью закрываем ColorPickerService после запуска overlay
                android.util.Log.d("GameInfoFragment", "Step 4: 🛑 Stopping ColorPickerService after overlay launch...");
                try {
               //     android.content.Intent colorPickerIntent = new android.content.Intent(getContext(), com.glass.engine.floating.ColorPickerService.class);
             //       getContext().stopService(colorPickerIntent);
                    android.util.Log.d("GameInfoFragment", "Step 4: ✅ ColorPickerService stopped");
                } catch (Exception e) {
                    android.util.Log.w("GameInfoFragment", "Step 4: ⚠️ Could not stop ColorPickerService: " + e.getMessage());
                }

            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "Step 4: ❌ Overlay and socket failed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Определяет и сохраняет оптимальный FPS на основе возможностей экрана
     */
    private void detectAndSaveOptimalFps() {
        try {
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping FPS detection");
                return;
            }

            android.view.WindowManager windowManager = (android.view.WindowManager) getContext().getSystemService(android.content.Context.WINDOW_SERVICE);
            android.view.Display display = windowManager.getDefaultDisplay();

            float maxRefreshRate = 60f; // По умолчанию 60Hz

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // Android R+ - используем getSupportedModes()
                android.view.Display.Mode[] modes = display.getSupportedModes();
                for (android.view.Display.Mode mode : modes) {
                    if (mode.getRefreshRate() > maxRefreshRate) {
                        maxRefreshRate = mode.getRefreshRate();
                    }
                }
                android.util.Log.d("GameInfoFragment", "🖥️ Max refresh rate detected: " + maxRefreshRate + "Hz");
            } else {
                // Android < R - используем getRefreshRate()
                maxRefreshRate = display.getRefreshRate();
                android.util.Log.d("GameInfoFragment", "🖥️ Current refresh rate: " + maxRefreshRate + "Hz");
            }

            // Определяем оптимальный FPS с поддержкой минимальных значений
            int optimalFps;
            if (maxRefreshRate >= 120f) {
                optimalFps = 120; // Максимум 120 FPS
                android.util.Log.d("GameInfoFragment", "🎯 Screen supports 120Hz+, using optimal: 120 FPS");
            } else if (maxRefreshRate >= 90f) {
                optimalFps = 90;
                android.util.Log.d("GameInfoFragment", "🎯 Screen supports 90Hz+, using optimal: 90 FPS");
            } else if (maxRefreshRate >= 60f) {
                optimalFps = 60;
                android.util.Log.d("GameInfoFragment", "🎯 Screen supports 60Hz+, using optimal: 60 FPS");
            } else if (maxRefreshRate >= 45f) {
                optimalFps = 45; // Для 45Hz экранов (некоторые бюджетные устройства)
                android.util.Log.d("GameInfoFragment", "🎯 Screen supports 45Hz, using optimal: 45 FPS");
            } else if (maxRefreshRate >= 30f) {
                optimalFps = 30; // Минимум 30 FPS для старых устройств
                android.util.Log.d("GameInfoFragment", "🎯 Screen supports 30Hz, using minimal: 30 FPS");
            } else {
                // Экстремально низкие refresh rates - используем 30 FPS как абсолютный минимум
                optimalFps = 30;
                android.util.Log.w("GameInfoFragment", "⚠️ Screen very low refresh rate (" + maxRefreshRate + "Hz), forcing minimal: 30 FPS");
            }

            // Сохраняем в SharedPreferences
            android.content.SharedPreferences prefs = com.glass.engine.BoxApplication.get().getApplicationContext()
                .getSharedPreferences("DisplaySettings", android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("optimalFps", optimalFps);
            editor.putFloat("maxRefreshRate", maxRefreshRate);
            editor.apply();

            android.util.Log.d("GameInfoFragment", "💾 Optimal FPS saved: " + optimalFps + " (max refresh: " + maxRefreshRate + "Hz)");
            android.util.Log.d("GameInfoFragment", "📊 FPS Detection Summary:");
            android.util.Log.d("GameInfoFragment", "   🖥️ Screen refresh rate: " + maxRefreshRate + "Hz");
            android.util.Log.d("GameInfoFragment", "   🎯 Optimal FPS chosen: " + optimalFps);
            android.util.Log.d("GameInfoFragment", "   💾 Saved to SharedPreferences: DisplaySettings");

            // Передаем FPS в ESPView.ChangeFps()
            try {
            //    com.glass.engine.floating.ESPView.ChangeFps(optimalFps);
                android.util.Log.d("GameInfoFragment", "✅ FPS applied to ESPView successfully!");
                android.util.Log.d("GameInfoFragment", "   📈 ESPView sleepTime: " + (1000 / optimalFps) + "ms");
            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "❌ Error applying FPS to ESPView: " + e.getMessage());
                android.util.Log.e("GameInfoFragment", "   🔄 Attempting fallback to 60 FPS...");
                try {
                //    com.glass.engine.floating.ESPView.ChangeFps(60);
                    android.util.Log.d("GameInfoFragment", "✅ Fallback 60 FPS applied successfully");
                } catch (Exception ex) {
                    android.util.Log.e("GameInfoFragment", "❌ Even fallback failed: " + ex.getMessage());
                }
            }

        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error detecting optimal FPS: " + e.getMessage());

            // Fallback - используем 60 FPS по умолчанию
            try {
            //    com.glass.engine.floating.ESPView.ChangeFps(60);
                android.util.Log.d("GameInfoFragment", "🔄 Fallback: Applied default 60 FPS to ESPView");
            } catch (Exception ex) {
                android.util.Log.e("GameInfoFragment", "❌ Error applying fallback FPS: " + ex.getMessage());
            }
        }
    }

    /**
     * Получает сохраненный оптимальный FPS
     */
    private int getSavedOptimalFps() {
        try {
            android.content.SharedPreferences prefs = com.glass.engine.BoxApplication.get().getApplicationContext()
                .getSharedPreferences("DisplaySettings", android.content.Context.MODE_PRIVATE);
            return prefs.getInt("optimalFps", 60); // По умолчанию 60 FPS
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error getting saved FPS: " + e.getMessage());
            return 60; // Fallback
        }
    }

    /**
     * Определяет задержку на основе FPS устройства
     */
    private int getDelayBasedOnFps() {
        try {
            android.content.SharedPreferences prefs = com.glass.engine.BoxApplication.get().getApplicationContext()
                .getSharedPreferences("DisplaySettings", android.content.Context.MODE_PRIVATE);
            int fps = prefs.getInt("optimalFps", 60);
            int delay;

            if (fps >= 120) {
                delay = 8000; // 7 секунд для 120+ FPS
                android.util.Log.d("GameInfoFragment", "⏱️ FPS " + fps + " → delay: 7 seconds");
            } else if (fps >= 90) {
                delay = 13000; // 12 секунд для 90+ FPS
                android.util.Log.d("GameInfoFragment", "⏱️ FPS " + fps + " → delay: 12 seconds");
            } else if (fps >= 60) {
                delay = 18000; // 18 секунд для 60/45 FPS
                android.util.Log.d("GameInfoFragment", "⏱️ FPS " + fps + " → delay: 18 seconds");
            } else {
                delay = 23000; // 18 секунд для 60/45 FPS
                android.util.Log.d("GameInfoFragment", "⏱️ FPS " + fps + " → delay: 18 seconds");
            }

            return delay;
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error calculating delay, using default 18s: " + e.getMessage());
            return 18000; // Fallback к 18 секундам
        }
    }

    /**
     * Запуск Color Picker для PUBG сразу после обнаружения
     */
    private void startColorPickerForPubg() {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping color picker");
                return;
            }

            android.util.Log.d("GameInfoFragment", "🎨 Starting Color Picker for PUBG immediately...");
            // Ждем разрешения перед продолжением
            downloadAndExtractPubgFiles(gamepkg);
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error starting color picker: " + e.getMessage());
        }
    }

    /**
     * Ожидание разрешения для Color Picker
     */
    private void waitForColorPickerPermission() {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping color picker permission");
                return;
            }

            android.util.Log.d("GameInfoFragment", "⏳ Waiting for Color Picker permission...");

            // Проверяем, есть ли уже разрешение
            if (android.provider.Settings.canDrawOverlays(getContext())) {
                android.util.Log.d("GameInfoFragment", "✅ Overlay permission already granted, requesting media projection...");
                requestMediaProjectionPermission();
            } else {
                // Запрашиваем разрешение на overlay
                android.util.Log.d("GameInfoFragment", "🔐 Requesting overlay permission...");
                android.content.Intent intent = new android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:" + getContext().getPackageName())
                );
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error in waitForColorPickerPermission: " + e.getMessage());
        }
    }

    /**
     * Запрос разрешения на Media Projection
     */
    private void requestMediaProjectionPermission() {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping media projection permission");
                return;
            }

            android.util.Log.d("GameInfoFragment", "📱 Requesting Media Projection permission...");
            startColorPickerService();
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error requesting media projection permission: " + e.getMessage());
        }
    }

    /**
     * Запуск Color Picker Service
     */
    private void startColorPickerService() {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping color picker service");
                return;
            }

            android.util.Log.d("GameInfoFragment", "🎨 Starting Color Picker Service...");

            // Запрашиваем Media Projection
            android.media.projection.MediaProjectionManager projectionManager =
                (android.media.projection.MediaProjectionManager) getContext().getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE);

            if (projectionManager != null) {
                android.content.Intent intent = projectionManager.createScreenCaptureIntent();
                startActivityForResult(intent, MEDIA_PROJECTION_REQUEST_CODE);
            } else {
                android.util.Log.e("GameInfoFragment", "❌ MediaProjectionManager not available");
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error starting color picker service: " + e.getMessage());
        }
    }

    // Константы для request codes
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1001;
    private static final int MEDIA_PROJECTION_REQUEST_CODE = 1002;

    @Override
    public void onResume() {
        super.onResume();

        // Проверяем, что фрагмент прикреплен к активности
        if (!isAdded() || getContext() == null) {
            android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping onResume");
            return;
        }

        android.util.Log.d("GameInfoFragment", "🔄 onResume - checking if need to reset slider");

        // Если слайдер в положении 100%, сбрасываем его и останавливаем сервисы
        if (isLaunched) {
            android.util.Log.d("GameInfoFragment", "🔄 onResume - slider is at 100%, resetting...");
            resetSliderAndStopServices();
        }

        // Регистрируем BroadcastReceiver для bypass trigger
        registerBypassReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Сбрасываем все флаги при уничтожении фрагмента
        isLaunched = false;
        android.util.Log.d("GameInfoFragment", "🔄 onDestroy - resetting all flags");

        // Убираем черный экран если он есть
        removeBlackOverlay();

        // Отменяем регистрацию BroadcastReceiver
        unregisterBypassReceiver();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping activity result");
                return;
            }

            if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
                if (android.provider.Settings.canDrawOverlays(getContext())) {
                    android.util.Log.d("GameInfoFragment", "✅ Overlay permission granted, requesting media projection...");
                  //  startColorPickerService();
                } else {
                    android.util.Log.w("GameInfoFragment", "⚠️ Overlay permission denied, requesting again...");
                    android.widget.Toast.makeText(getContext(), "Overlay permission required! Please grant permission and try again.", android.widget.Toast.LENGTH_SHORT).show();

                    // Повторно запрашиваем разрешение через 2 секунды
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        if (isAdded()) {
                            android.util.Log.d("GameInfoFragment", "🔄 Retrying overlay permission request...");
                        //    waitForColorPickerPermission();
                        }
                    }, 2000);
                }
            } else  {
                 
                    // Запускаем PUBG download после получения разрешения
                  // String packageName = "com.miniclip.eightballpool"; // PUBG package
                 //   downloadAndExtractPubgFiles(gamepkg);

                
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error in onActivityResult: " + e.getMessage());
        }
    }

    /**
     * Делает экран полностью черным
     */
    private void makeScreenBlack() {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping black screen");
                return;
            }
            
            android.util.Log.d("GameInfoFragment", "🎨 Making screen black immediately");
            
            // Показываем черный фрагмент поверх всего
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    try {
                        // Создаем простой черный фрагмент
                        BlackScreenFragment blackFragment = new BlackScreenFragment();
                        
                        // Показываем черный фрагмент поверх текущего
                        getParentFragmentManager().beginTransaction()
                            .add(android.R.id.content, blackFragment, "BLACK_SCREEN")
                            .addToBackStack(null)
                            .commit();
                        
                        android.util.Log.d("GameInfoFragment", "✅ Black fragment shown over entire screen");
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "❌ Error showing black fragment: " + e.getMessage());
                    }
                });
            }
            
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error making screen black: " + e.getMessage());
        }
    }

    /**
     * Убирает черный фон с экрана
     */
    private void removeBlackOverlay() {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping black screen removal");
                return;
            }
            
            android.util.Log.d("GameInfoFragment", "🎨 Removing black screen fragment");
            
            // Убираем черный фрагмент
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    try {
                        // Находим и удаляем черный фрагмент
                        androidx.fragment.app.Fragment blackFragment = getParentFragmentManager().findFragmentByTag("BLACK_SCREEN");
                        if (blackFragment != null) {
                            getParentFragmentManager().beginTransaction()
                                .remove(blackFragment)
                                .commit();
                            
                            android.util.Log.d("GameInfoFragment", "✅ Black screen fragment removed");
                        }
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "❌ Error removing black fragment: " + e.getMessage());
                    }
                });
            }
            
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error removing black screen: " + e.getMessage());
        }
    }

    /**
     * Запускает таймер для изменения ориентации через 2 секунды после запуска игры
     */
    private void startPortraitOrientationTimer() {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping orientation timer");
                return;
            }
            
            android.util.Log.d("GameInfoFragment", "🎮 Starting orientation change timer after game launch");
            
            // Через 3 секунды после открытия игры меняем ориентацию
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (isAdded() && getContext() != null) {
                        // Сначала разблокируем ориентацию
                        if (getContext() instanceof com.glass.engine.activity.MainActivity) {
                            com.glass.engine.activity.MainActivity activity = (com.glass.engine.activity.MainActivity) getContext();
                            activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                            android.util.Log.d("GameInfoFragment", "✅ Step 1: Orientation unlocked after 2 seconds");
                            
                            // Сразу блокируем заново в портретной ориентации
                            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                try {
                                    if (isAdded() && getContext() != null) {
                                        activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                                        android.util.Log.d("GameInfoFragment", "✅ Step 2: Screen locked in portrait orientation");
                                        
                                        // Сбрасываем флаг ландшафтной блокировки
                                        com.glass.engine.activity.MainActivity.setLandscapeLockActive(getContext(), false);
                                        
                                        // Убираем черный фон после изменения ориентации
                                        removeBlackOverlay();
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("GameInfoFragment", "❌ Error locking portrait: " + e.getMessage());
                                }
                            }, 100); // Через 100мс блокируем в портретной
                            
                        }
                        android.util.Log.d("GameInfoFragment", "✅ Orientation change timer completed");
                    }
                } catch (Exception e) {
                    android.util.Log.e("GameInfoFragment", "❌ Error in orientation timer: " + e.getMessage());
                }
            }, 3000); // Ждем 3 секунды
            
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error starting orientation timer: " + e.getMessage());
        }
    }

    /**
     * Сбрасывает слайдер в начальное положение и останавливает все сервисы
     */
    private void resetSliderAndStopServices() {
        try {
            // Проверяем, что фрагмент прикреплен к активности
            if (!isAdded() || getContext() == null) {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment not attached, skipping slider reset");
                return;
            }
            
            android.util.Log.d("GameInfoFragment", "🔄 Resetting slider and stopping all services");
            
            // Сначала убираем черный экран если он есть
            removeBlackOverlay();
            
            // Сбрасываем слайдер в начальное положение
            View sliderContainer = requireView().findViewById(R.id.custom_slider_container);
            View sliderThumb = requireView().findViewById(R.id.btnSignIn);
            TextView sliderLabel = requireView().findViewById(R.id.slider_label);
            
            if (sliderThumb != null && sliderLabel != null) {
                // Анимируем возврат слайдера в начальное положение
                sliderThumb.animate()
                    .x(0) // Возвращаем в начало
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                    .withEndAction(() -> {
                        // Сбрасываем флаг запуска
                        isLaunched = false;
                        
                        // Меняем текст на "slide to stop" и делаем его красным
                        sliderLabel.setText("slide to stop");
                        sliderLabel.setTextColor(android.graphics.Color.RED);
                        sliderLabel.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                        applyWhiteGradientToText(sliderLabel);
                        
                        android.util.Log.d("GameInfoFragment", "✅ Slider reset to initial position with 'slide to stop' text");
                    })
                    .start();
            }
            
            // Останавливаем все сервисы и процессы
            stopAllServices();
            
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error resetting slider: " + e.getMessage());
        }
    }

    /**
     * Останавливает все запущенные сервисы и процессы
     */
    private void stopAllServices() {
        try {
            android.util.Log.d("GameInfoFragment", "🛑 Stopping all services and processes");
            
            // Сбрасываем флаги ColorPickerService
            resetBypassFlags();
            
            // Останавливаем overlay сервис
            try {
          //      android.content.Intent overlayIntent = new android.content.Intent(getContext(), com.glass.engine.floating.Overlay.class);
            //    getContext().stopService(overlayIntent);
                android.util.Log.d("GameInfoFragment", "✅ Overlay service stopped");
            } catch (Exception e) {
                android.util.Log.w("GameInfoFragment", "⚠️ Could not stop overlay service: " + e.getMessage());
            }
            
            // Останавливаем Color Picker сервис
            try {
            //    android.content.Intent colorPickerIntent = new android.content.Intent(getContext(), com.glass.engine.floating.ColorPickerService.class);
           //     getContext().stopService(colorPickerIntent);
                android.util.Log.d("GameInfoFragment", "✅ Color Picker service stopped");
            } catch (Exception e) {
                android.util.Log.w("GameInfoFragment", "⚠️ Could not stop color picker service: " + e.getMessage());
            }
            
            // Убиваем sock cheat процессы
            try {
                Runtime.getRuntime().exec("pkill -f pubg_sock");
                Runtime.getRuntime().exec("killall pubg_sock");
                android.util.Log.d("GameInfoFragment", "✅ Sock cheat processes killed");
            } catch (Exception e) {
                android.util.Log.w("GameInfoFragment", "⚠️ Could not kill sock processes: " + e.getMessage());
            }
            
            // Сбрасываем все флаги
            try {
                // Сбрасываем флаг ландшафтной блокировки
                com.glass.engine.activity.MainActivity.setLandscapeLockActive(getContext(), false);
                android.util.Log.d("GameInfoFragment", "✅ Landscape lock flag reset");
            } catch (Exception e) {
                android.util.Log.w("GameInfoFragment", "⚠️ Could not reset landscape flag: " + e.getMessage());
            }
            
            android.util.Log.d("GameInfoFragment", "✅ All services and processes stopped");
            
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error stopping services: " + e.getMessage());
        }
    }
    
    /**
     * Регистрирует BroadcastReceiver для получения сигнала запуска bypass
     */
    private void registerBypassReceiver() {
        try {
            if (bypassTriggerReceiver == null && getContext() != null) {
                bypassTriggerReceiver = new android.content.BroadcastReceiver() {
                    @Override
                    public void onReceive(android.content.Context context, android.content.Intent intent) {
                        if ("com.glass.engine.TRIGGER_BYPASS".equals(intent.getAction())) {
                            android.util.Log.d("GameInfoFragment", "🎯 Received bypass trigger from ColorPickerService");
                            
                            // Получаем информацию о текущей игре
                            String currentPackage = getCurrentGamePackage();
                            boolean isRoot = isRootMode();
                            
                            android.util.Log.d("GameInfoFragment", "🎯 Bypass trigger received!");
                            android.util.Log.d("GameInfoFragment", "📱 Current package from args: " + currentPackage);
                            android.util.Log.d("GameInfoFragment", "🔧 Root mode: " + isRoot);
                            
                            // Определяем тип игры по package name
                            if (currentPackage != null) {
                                if (currentPackage.equals("jdjcjnc") || currentPackage.equals("com.garena.gme.df")) {
                                    android.util.Log.d("GameInfoFragment", "🎮 Detected DELTA FORCE game, starting delta bypass...");
                                    startDeltaBypassTriggered(isRoot, currentPackage);
                                } else if (currentPackage.equals("com.miniclip.eightballpool") || currentPackage.equals("com.pubg.krmobile") || currentPackage.equals("com.vng.pubgmobile") || currentPackage.equals("com.rekoo.pubgm")) {
                                    android.util.Log.d("GameInfoFragment", "🎮 Detected PUBG game, starting PUBG bypass...");
                                 //   startPubgBypassTriggered(isRoot, currentPackage);
                                } else {
                                    android.util.Log.w("GameInfoFragment", "⚠️ Unknown game package: " + currentPackage + ", cannot determine bypass type");
                                }
                            } else {
                                android.util.Log.e("GameInfoFragment", "❌ No game package found in fragment arguments!");
                            }
                        }
                    }
                };
                
                android.content.IntentFilter filter = new android.content.IntentFilter("com.glass.engine.TRIGGER_BYPASS");
                getContext().registerReceiver(bypassTriggerReceiver, filter);
                android.util.Log.d("GameInfoFragment", "✅ Bypass receiver registered");
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error registering bypass receiver: " + e.getMessage());
        }
    }
    
    /**
     * Отменяет регистрацию BroadcastReceiver
     */
    private void unregisterBypassReceiver() {
        try {
            if (bypassTriggerReceiver != null && getContext() != null) {
                getContext().unregisterReceiver(bypassTriggerReceiver);
                bypassTriggerReceiver = null;
                android.util.Log.d("GameInfoFragment", "✅ Bypass receiver unregistered");
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error unregistering bypass receiver: " + e.getMessage());
        }
    }
    
    /**
     * Получает package name текущей игры из аргументов фрагмента
     */
    private String getCurrentGamePackage() {
        try {
            Bundle args = getArguments();
            android.util.Log.d("GameInfoFragment", "🔍 Getting game package from fragment arguments...");
            
            if (args != null) {
                String packageName = args.getString("gamepackagename", null);
                android.util.Log.d("GameInfoFragment", "📦 Package from args: " + packageName);
                return packageName;
            } else {
                android.util.Log.w("GameInfoFragment", "⚠️ Fragment arguments are null");
            }
        } catch (Exception e) {
            android.util.Log.e("GameInfoFragment", "❌ Error getting current game package: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Запуск delta bypass по триггеру (без таймера)
     */
    private void startDeltaBypassTriggered(boolean isRoot, String packageName) {
        android.util.Log.d("GameInfoFragment", "🎯 TRIGGERED Delta bypass (no timer) - root: " + isRoot + ", package: " + packageName);
        
        new Thread(() -> {
            try {
                // Убираем начальную задержку - запускаем сразу
                android.util.Log.d("GameInfoFragment", "🚀 TRIGGERED bypass starting immediately...");
                
                // Определяем путь к bypass файлу в зависимости от режима
                String bypassPath;
                if (isRoot) {
                    bypassPath = "/data/local/tmp/delta_rootbypass";
                } else {
                    bypassPath = com.glass.engine.BoxApplication.get().getApplicationContext().getFilesDir().getAbsolutePath() + "/delta_bypass";
                }
                
                // Проверяем существование файла
                File bypassFile = new File(bypassPath);
                if (!bypassFile.exists()) {
                    android.util.Log.e("GameInfoFragment", "❌ TRIGGERED bypass file not found: " + bypassPath);
                    return;
                }
                
                // Даем разрешение на выполнение
                if (isRoot) {
                    Runtime.getRuntime().exec("su -c 'chmod +x " + bypassPath + "'");
                } else {
                    Runtime.getRuntime().exec("chmod +x " + bypassPath);
                }
                Thread.sleep(500);
                
                // Запускаем bypass 3 раза каждые 1 секунду (без начальной задержки)
                for (int i = 1; i <= 3; i++) {
                    android.util.Log.d("GameInfoFragment", "🎯 TRIGGERED Delta bypass attempt " + i + "/3");
                    
                    try {
                        String command;
                        if (isRoot) {
                            command = "su -c '" + bypassPath + " " + packageName + "'";
                        } else {
                            command = bypassPath + " " + packageName;
                        }
                        
                        Process process = Runtime.getRuntime().exec(command);
                        int exitCode = process.waitFor();
                        android.util.Log.d("GameInfoFragment", "✅ TRIGGERED Delta bypass " + i + " completed (exit: " + exitCode + ")");
                        
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "❌ TRIGGERED Delta bypass " + i + " failed: " + e.getMessage());
                    }
                    
                    // Ждем 1 секунду перед следующей попыткой (кроме последней)
                    if (i < 3) {
                        Thread.sleep(1000);
                    }
                }
                
                android.util.Log.d("GameInfoFragment", "🎉 TRIGGERED Delta bypass sequence completed!");
                
                // Запуск overlay и socket после завершения bypass
                startOverlayAndSocket(isRoot, packageName);
                
            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "❌ TRIGGERED Delta bypass failed: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Запуск PUBG bypass по триггеру (без таймера)
     */
    private void startPubgBypassTriggered(boolean isRoot, String packageName) {
        android.util.Log.d("GameInfoFragment", "🎯 TRIGGERED PUBG bypass (no timer) - root: " + isRoot + ", package: " + packageName);
        
        new Thread(() -> {
            try {
                // Убираем начальную задержку - запускаем сразу
                android.util.Log.d("GameInfoFragment", "🚀 TRIGGERED PUBG bypass starting immediately...");
                
                // Определяем путь к bypass файлу в зависимости от режима
                String bypassPath;
                if (isRoot) {
                    bypassPath = "/data/local/tmp/pubg_rootbypass";
                } else {
                    bypassPath = com.glass.engine.BoxApplication.get().getApplicationContext().getFilesDir().getAbsolutePath() + "/pubg_bypass";
                }
                
                // Проверяем существование файла
                File bypassFile = new File(bypassPath);
                if (!bypassFile.exists()) {
                    android.util.Log.e("GameInfoFragment", "❌ TRIGGERED PUBG bypass file not found: " + bypassPath);
                    return;
                }
                
                // Даем разрешение на выполнение
                if (isRoot) {
                    Runtime.getRuntime().exec("su -c 'chmod +x " + bypassPath + "'");
                } else {
                    Runtime.getRuntime().exec("chmod +x " + bypassPath);
                }
                Thread.sleep(500);
                
                // Запускаем bypass 3 раза каждые 1 секунду (без начальной задержки)
                for (int i = 1; i <= 3; i++) {
                    android.util.Log.d("GameInfoFragment", "🎯 TRIGGERED PUBG bypass attempt " + i + "/3");
                    
                    try {
                        String command;
                        if (isRoot) {
                            command = "su -c '" + bypassPath + "'004'";
                        } else {
                            command = bypassPath + "'004'";
                        }
                        
                        Process process = Runtime.getRuntime().exec(command);
                        int exitCode = process.waitFor();
                        android.util.Log.d("GameInfoFragment", "✅ TRIGGERED PUBG bypass " + i + " completed (exit: " + exitCode + ")");
                        
                    } catch (Exception e) {
                        android.util.Log.e("GameInfoFragment", "❌ TRIGGERED PUBG bypass " + i + " failed: " + e.getMessage());
                    }
                    
                    // Ждем 1 секунду перед следующей попыткой (кроме последней)
                    if (i < 3) {
                        Thread.sleep(1000);
                    }
                }
                
                android.util.Log.d("GameInfoFragment", "🎉 TRIGGERED PUBG bypass sequence completed!");
                
                // Запуск PUBG overlay и socket после завершения bypass
                startPubgOverlayAndSocket(isRoot);
                
            } catch (Exception e) {
                android.util.Log.e("GameInfoFragment", "❌ TRIGGERED PUBG bypass failed: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Простой черный фрагмент для покрытия всего экрана
     */
    public static class BlackScreenFragment extends androidx.fragment.app.Fragment {
        
        @Override
        public android.view.View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, android.os.Bundle savedInstanceState) {
            // Создаем простой черный view на весь экран
            android.view.View blackView = new android.view.View(requireContext());
            blackView.setBackgroundColor(android.graphics.Color.BLACK);
            
            // Устанавливаем размеры на весь экран
            android.view.ViewGroup.LayoutParams params = new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            blackView.setLayoutParams(params);
            
            return blackView;
        }
        
        @Override
        public void onViewCreated(android.view.View view, android.os.Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            android.util.Log.d("BlackScreenFragment", "🎨 Black screen fragment created and displayed");
        }
    }
}