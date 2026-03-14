package com.glass.engine.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.glass.engine.R;
import com.glass.engine.adapter.GameAdapter;
import com.glass.engine.model.GameModel;
import com.glass.engine.utils.DeviceUtils;

import java.util.ArrayList;
import java.util.List;

import com.vbox.VBoxCore;

public class MainContentFragment extends Fragment {

    private RecyclerView recyclerView;
    private GameAdapter adapter;
    private List<GameModel> allGames = new ArrayList<>();
    private MaterialButton lastSelectedButton;
    private TextView gamesLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_content, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        gamesLabel = view.findViewById(R.id.games_label);

        allGames = getDefaultGames();

        adapter = new GameAdapter(allGames, requireActivity().getSupportFragmentManager());

        int spanCount;
        if (DeviceUtils.isTablet(getContext())) {
            boolean isLandscape = getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
            spanCount = isLandscape ? 5 : 3;
        } else {
            spanCount = 2;
        }
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        recyclerView.setAdapter(adapter);

        adapter.setOnGetClickListener(gameModel -> {
            GameInfoFragment fragment = GameInfoFragment.newInstance(
                gameModel.getGamenameincard(),
                gameModel.getGameversionincard(),
                gameModel.getGamecardimage(),
                gameModel.getGamecardoverlayimage(),
                gameModel.getFragmentimage(),
                gameModel.getGameIcon(),
                gameModel.getFragmentgamename(),
                gameModel.getFragmentpublisher(),
                gameModel.getFragmentdescription(),
                gameModel.getBasicDescription(),
                gameModel.getAdvancedDescription(),
                gameModel.getUltimateDescription(),
                gameModel.getGamepackagename()
            );
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.drawer_layout, fragment)
                .addToBackStack(null)
                .commit();
        });

        
        runCardAnimation();

        return view;
    }

    private List<GameModel> getDefaultGames() {
        java.util.List<GameModel> games = new java.util.ArrayList<>();


        games.add(GameModel.AddGame(
            new String[]{"8 Ball Pool", "Pool Game"},           // карточка
            new int[]{R.drawable.ball8, 0},                // изображения карточки
            R.drawable.deltagarena,                          // фоновое изображение фрагмента
            R.drawable.ball8,                              // game_icon (используем то же изображение)
            new String[]{
                
                getString(R.string.eight_ball_pool),         // "8 Ball Pool"
                getString(R.string.by_x_project),            // "By X-Project"  
                getString(R.string.eight_ball_pool_description), // основное описание
                getString(R.string.eight_ball_pool_ultimate_description), // описание для Basic
                getString(R.string.eight_ball_pool_ultimate_description), // описание для Advanced
                getString(R.string.eight_ball_pool_ultimate_description), // описание для Ultimate
                "com.miniclip.eightballpool"                             // пакет
            },
            true, true, true, true                          // флаги
        ));
         


        return games;
    }

    
    

    private void animateGamesLabel(String text) {
        gamesLabel.setText(text);
        Animation animation = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
        gamesLabel.startAnimation(animation);
    }

    private void runCardAnimation() {
        recyclerView.post(() -> {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager == null) return;

            for (int i = 0; i < recyclerView.getChildCount(); i++) {
                View child = recyclerView.getChildAt(i);
                child.setAlpha(0f);
                child.animate()
                    .alpha(1f)
                    .setStartDelay(i * 100L)
                    .setDuration(400)
                    .start();
            }
        });
    }



    private void applyFadeIn(View view) {
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        animation.setFillAfter(true);
        view.startAnimation(animation);
    }
}
