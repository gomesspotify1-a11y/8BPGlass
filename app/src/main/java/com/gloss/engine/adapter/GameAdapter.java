package com.glass.engine.adapter;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glass.engine.R;
import com.glass.engine.model.GameModel;

import java.util.List;

import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

public class GameAdapter extends RecyclerView.Adapter<GameAdapter.GameViewHolder> {

    private int lastVisiblePosition = -1;
    private boolean animationsPlayed = false;

    public interface OnGetClickListener {
        void onGetClick(GameModel game);
    }

    private OnGetClickListener onGetClickListener;

    public void setOnGetClickListener(OnGetClickListener listener) {
        this.onGetClickListener = listener;
    }

    private List<GameModel> gameList;

    public GameAdapter(List<GameModel> gameList, FragmentManager fragmentManager) {
        this.gameList = gameList;
    }

    /**
     * @noinspection ClassEscapesDefinedScope
     */
    @NonNull
    @Override
    public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.game_item, parent, false);
        return new GameViewHolder(view);
    }

    /**
     * @noinspection ClassEscapesDefinedScope
     */
    // RecyclerView reference for direct ViewHolder access
    private RecyclerView recyclerView;

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public void onBindViewHolder(@NonNull GameViewHolder holder, @SuppressLint("RecyclerView") int position) {
        GameModel game = gameList.get(position);
        holder.name.setText(game.getGamenameincard());
        holder.version.setText(game.getGameversionincard());

        Drawable image = ContextCompat.getDrawable(holder.itemView.getContext(), game.getGamecardimage());
        if (image != null) {
            holder.image.setImageDrawable(image);
        }

        if (game.getGamecardoverlayimage() != 0) {
            Drawable overlay = ContextCompat.getDrawable(holder.itemView.getContext(), game.getGamecardoverlayimage());
            if (overlay != null) {
                holder.overlayIcon.setVisibility(View.VISIBLE);
                holder.overlayIcon.setImageDrawable(overlay);
            } else {
                holder.overlayIcon.setVisibility(View.GONE);
            }
        } else {
            holder.overlayIcon.setVisibility(View.GONE);
        }

        // Animation animation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_scroll_anim);
        // holder.itemView.startAnimation(animation);

        BlurView blurViewOverlay = holder.itemView.findViewById(R.id.blurViewCards);
        ViewGroup rootView = (ViewGroup) holder.itemView;
        Drawable windowBackground = holder.itemView.getContext().getDrawable(android.R.color.transparent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            blurViewOverlay.setupWith(rootView)
                    .setFrameClearDrawable(windowBackground)
                    .setBlurRadius(15f)
                    .setBlurAutoUpdate(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            blurViewOverlay.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            blurViewOverlay.setClipToOutline(true);
        }
        holder.getButton.setOnClickListener(v -> {
            if (onGetClickListener != null) {
                onGetClickListener.onGetClick(game);
            }
        });

        FrameLayout passwordLayout = holder.itemView.findViewById(R.id.passwordLayout);
        if (!animationsPlayed) {
            passwordLayout.setVisibility(View.INVISIBLE);
            passwordLayout.post(() -> {
                passwordLayout.setTranslationY(passwordLayout.getHeight());
                passwordLayout.setVisibility(View.VISIBLE);
                passwordLayout.animate()
                        .translationY(0)
                        .setDuration(400)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(() -> animationsPlayed = true)
                        .start();
            });
        } else {
            passwordLayout.setVisibility(View.VISIBLE);
            passwordLayout.setTranslationY(0);
        }

        holder.itemView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (lastVisiblePosition != position) {
                    lastVisiblePosition = position;
                }
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return gameList.size();
    }

    static class GameViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        ImageView overlayIcon;
        TextView name, version;
        TextView getButton;

        GameViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.gamecardimage);
            overlayIcon = itemView.findViewById(R.id.gamecardoverlayimage);
            name = itemView.findViewById(R.id.gamenameincard);
            version = itemView.findViewById(R.id.gameversionincard);
            getButton = itemView.findViewById(R.id.get_button);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<GameModel> newList) {
        this.gameList = newList;
        notifyDataSetChanged();
    }
    /**
     * Hides the currently visible password layout, if any.
     */
    public void clearVisibleLayout() {
        if (lastVisiblePosition != -1) {
            int oldPosition = lastVisiblePosition;
            lastVisiblePosition = -1;
            notifyItemChanged(oldPosition);
        }
    }
}