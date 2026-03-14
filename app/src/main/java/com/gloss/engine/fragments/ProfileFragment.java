package com.glass.engine.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glass.engine.R;
import com.glass.engine.adapter.AppAdapter;

public class ProfileFragment extends Fragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                private long lastBackPressedTime = 0;

                @Override
                public void handleOnBackPressed() {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastBackPressedTime < 2000) {
                        requireActivity().finishAffinity();
                    } else {
                        lastBackPressedTime = currentTime;
                        Toast.makeText(requireContext(), getString(R.string.press_back_again_to_exit), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        populateDeviceInfo(view);
        return view;
    }

    private void populateDeviceInfo(View view) {
        String rawDate = com.glass.engine.activity.MainActivity.exdate();
        ((android.widget.TextView) view.findViewById(R.id.device_time)).setText(formatLicenseDate(rawDate));
        ((android.widget.TextView) view.findViewById(R.id.device_root)).setText(getString(R.string.root_access) + (isDeviceRooted() ? getString(R.string.yes) : getString(R.string.no)));
        ((android.widget.TextView) view.findViewById(R.id.device_os_version)).setText(getString(R.string.os_version) + android.os.Build.VERSION.RELEASE);
        ((android.widget.TextView) view.findViewById(R.id.device_name)).setText(getString(R.string.device_name) + android.os.Build.DEVICE);
        ((android.widget.TextView) view.findViewById(R.id.device_ram)).setText(getString(R.string.ram) + getTotalRAM());
    }

    private String formatLicenseDate(String rawDateTime) {
        try {
            // Обрезаем миллисекунды, если есть
            String clean = rawDateTime.split("\\.")[0];
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(clean);
            // Используем локаль устройства для перевода месяцев
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM d, HH:mm", java.util.Locale.getDefault());
            String formatted = dateTime.format(formatter);
            // Переставляем месяц и день местами для "July 23, 08:48"
            String[] parts = formatted.split(",");
            String[] monthDay = parts[0].split(" ");
            String result = getString(R.string.expires) + ": " + monthDay[0] + " " + monthDay[1] + "," + parts[1];
            return result.trim();
        } catch (Exception e) {
            return getString(R.string.expires) + ": " + rawDateTime;
        }
    }

    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
    }

    private boolean isDeviceRooted() {
        String[] paths = {
            "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"
        };
        for (String path : paths) {
            if (new java.io.File(path).exists()) return true;
        }
        return false;
    }

    private String getTotalRAM() {
        try {
            android.app.ActivityManager actManager = (android.app.ActivityManager) requireContext().getSystemService(android.content.Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo memInfo = new android.app.ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);
            long totalMem = memInfo.totalMem / (1024 * 1024); // in MB
            return totalMem + " MB";
        } catch (Exception e) {
            return "Unavailable";
        }
    }
}