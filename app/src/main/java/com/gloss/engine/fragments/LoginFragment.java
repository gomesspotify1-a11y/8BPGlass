package com.glass.engine.fragments;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.glass.engine.BoxApplication;
import com.glass.engine.R;
import com.glass.engine.activity.MainActivity;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

public class LoginFragment extends Fragment {

    static {
        try {
            System.loadLibrary("client");
        } catch (UnsatisfiedLinkError ignored) {}
    }

    private EditText textUsername;
    private ImageView btnSignIn;

    // 🔥 SAME AS LoginActivity
    private ProgressBar progressBar;
    private ImageView progressLogo;
    private View progressOverlay;

    private SharedPreferences loginPrefs;
    
    private static native String Check(Context context, String userKey);

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.activity_login, container, false);
        initViews(view);
        loadSavedKeyAndAutoLogin();
        setupLogin();
        return view;
    }

    private void initViews(View view) {
        textUsername = view.findViewById(R.id.textUsername);
        btnSignIn = view.findViewById(R.id.btnSignIn);

        progressBar = view.findViewById(R.id.circularProgressBar);
        progressLogo = view.findViewById(R.id.progressLogo);
        progressOverlay = view.findViewById(R.id.progressOverlay);

        loginPrefs = requireContext()
                .getSharedPreferences("login_prefs", Context.MODE_PRIVATE);

        // Initial state (VERY IMPORTANT)
        if (progressOverlay != null) progressOverlay.setVisibility(View.GONE);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
        }
        if (progressLogo != null) {
            progressLogo.setVisibility(View.VISIBLE);
            progressLogo.setImageResource(R.drawable.logo);
        }
    }

    // ---------------- AUTO LOGIN ----------------
    private void loadSavedKeyAndAutoLogin() {
        String savedKey = loginPrefs.getString("user_key", "");
        boolean loginSuccess = loginPrefs.getBoolean("login_success", false);

        if (!savedKey.isEmpty()) {
            textUsername.setText(savedKey);
            if (loginSuccess) {
                textUsername.post(() -> startLogin(savedKey));
            }
        }
    }

    private void setupLogin() {
        btnSignIn.setOnClickListener(v -> {
            String key = textUsername.getText().toString().trim();
            if (key.isEmpty()) {
                textUsername.setError("Field cannot be empty");
                return;
            }
            startLogin(key);
        });
    }

    // ---------------- LOGIN START (FROM ACTIVITY) ----------------
    private void startLogin(String key) {
        btnSignIn.setEnabled(false);
        textUsername.setEnabled(false);

        // Disable whole UI
        View root = requireView().findViewById(R.id.rootLayout);
        if (root != null) root.setEnabled(false);

        // 🔥 SHOW OVERLAY
        if (progressOverlay != null) {
            progressOverlay.setAlpha(1f);
            progressOverlay.setVisibility(View.VISIBLE);
        }

        if (progressLogo != null) {
            progressLogo.setImageResource(R.drawable.logo);
            progressLogo.setAlpha(1f);
        }

        if (progressBar != null) {
            progressBar.setAlpha(1f);
            progressBar.setProgress(0);
        }

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {

                if (msg.what == 0) {
                  
                    loginPrefs.edit()
                            .putString("user_key", key)
                            .putBoolean("login_success", true)
                            .apply();

                    // Tick animation
                    if (progressLogo != null) {
                        progressBar.animate().alpha(0f).setDuration(300).start();
                        progressLogo.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                            progressLogo.setImageResource(R.drawable.tick_circle);
                            progressLogo.setAlpha(0f);
                            progressLogo.animate().alpha(1f).setDuration(300).withEndAction(() -> {
                                new Handler().postDelayed(() -> {
                                    MainActivity.goMain(requireContext());
                                    
                                }, 800);
                            }).start();
                        }).start();
                    }
                } else {
                    // ❌ FAIL
                    showFailState(msg.obj);
                }
            }
        };

        new Thread(() -> {
            try {
                // SAME PROGRESS FLOW AS ACTIVITY
                for (int p = 0; p <= 100; p += 10) {
                    int value = p;
                    requireActivity().runOnUiThread(() ->
                            animateProgress(value));
                    Thread.sleep(100);
                }

                BoxApplication.copyCacertFromAssetsIfNeeded(requireContext());
                String result = Check(requireContext(), key);

                Message msg = new Message();
                if ("OK".equals(result)) {
                    saveLicence(key);
                    msg.what = 0;
                } else {
                    msg.what = 1;
                    msg.obj = result;
                }
                handler.sendMessage(msg);

            } catch (Exception e) {
                Message msg = new Message();
                msg.what = 1;
                msg.obj = e.getMessage();
                handler.sendMessage(msg);
            }
        }).start();
    }

    // ---------------- FAIL STATE ----------------
    private void showFailState(Object error) {
        if (progressLogo != null) {
            progressBar.animate().alpha(0f).setDuration(300).start();
            progressLogo.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                progressLogo.setImageResource(R.drawable.minus_cirlce);
                progressLogo.setAlpha(0f);
                progressLogo.animate().alpha(1f).setDuration(300).withEndAction(() -> {
                    new Handler().postDelayed(this::restoreUI, 1200);
                }).start();
            }).start();
        }

        Toast.makeText(requireContext(),
                String.valueOf(error),
                Toast.LENGTH_LONG).show();
    }

    // ---------------- RESTORE UI ----------------
    private void restoreUI() {
        if (progressOverlay != null) {
            progressOverlay.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> progressOverlay.setVisibility(View.GONE))
                    .start();
        }

        btnSignIn.setEnabled(true);
        textUsername.setEnabled(true);

        View root = requireView().findViewById(R.id.rootLayout);
        if (root != null) root.setEnabled(true);
    }

    // ---------------- PROGRESS ANIMATION ----------------
    private void animateProgress(int target) {
        ObjectAnimator anim = ObjectAnimator.ofInt(
                progressBar,
                "progress",
                progressBar.getProgress(),
                target
        );
        anim.setDuration(300);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    // ---------------- SAVE LICENCE ----------------
    private void saveLicence(String key) {
        try {
            JSONObject json = new JSONObject();
            json.put("licence", key);

            File dir = new File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOCUMENTS),
                    "Xproject"
            );
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "Licence.json");
            FileWriter writer = new FileWriter(file, false);
            writer.write(json.toString());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
