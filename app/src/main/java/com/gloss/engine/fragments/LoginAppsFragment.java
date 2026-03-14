package com.glass.engine.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.view.animation.DecelerateInterpolator;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glass.engine.R;
import com.glass.engine.adapter.AppAdapter;

public class LoginAppsFragment extends Fragment {

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
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.app_recycler_view);
        AppAdapter adapter = new AppAdapter(requireContext());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));

        if (view instanceof ViewGroup) {
            ViewGroup root = (ViewGroup) view;
            root.setAlpha(0f);
            root.animate().alpha(1f).setDuration(500).setInterpolator(new DecelerateInterpolator()).start();
        }

        return view;
    }
}