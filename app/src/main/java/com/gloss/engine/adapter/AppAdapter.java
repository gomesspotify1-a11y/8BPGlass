package com.glass.engine.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.glass.engine.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vbox.VBoxCore;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    private final List<AppModel> installedApps;
private static final int USER_ID = 0; // BlackBox default user
    @SuppressLint("UseCompatLoadingForDrawables")
    public AppAdapter(Context context) {
        List<AppModel> allApps = Arrays.asList(
    new AppModel(context.getString(R.string.twitter), context.getString(R.string.twitter_package)), 
/*    new AppModel(context.getString(R.string.facebook), context.getString(R.string.facebook_package)), 
    new AppModel(context.getString(R.string.wechat), context.getString(R.string.wechat_package)), 
    new AppModel(context.getString(R.string.qq), context.getString(R.string.qq_package)), 
    new AppModel(context.getString(R.string.vk), context.getString(R.string.vk_package)), */
    new AppModel(context.getString(R.string.via), context.getString(R.string.via_package))
);

        PackageManager pm = context.getPackageManager();
        installedApps = new ArrayList<>();
        for (AppModel app : allApps) {
            try {
                ApplicationInfo ai = pm.getApplicationInfo(app.packageName, 0);
                app.icon = pm.getApplicationIcon(ai);
                app.isInstalled = true;
            } catch (PackageManager.NameNotFoundException e) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    app.icon = context.getDrawable(R.drawable.not_ic);
                } else {
                    app.icon = context.getResources().getDrawable(R.drawable.not_ic);
                }
                app.isInstalled = false;
            }
            installedApps.add(app);
        }
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppModel app = installedApps.get(position);
        holder.name.setText(app.name);
        holder.icon.setImageDrawable(app.icon);

        if (!app.isInstalled && !app.wasCloned) {
            holder.icon.setColorFilter(0xFF888888);
        } else {
            holder.icon.clearColorFilter();
        }

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();

            boolean isClonedInstalled = false;
            try {
                isClonedInstalled = VBoxCore.get().isInstalled(app.packageName, USER_ID);

            } catch (Exception e) {

            }

            if (isClonedInstalled) {
                try {
                    VBoxCore.get().launchApk(app.packageName, USER_ID);
                } catch (Exception e) {
                    Toast.makeText(context, context.getString(R.string.error_launching_app, app.name), Toast.LENGTH_SHORT).show();
                }
                return;
            }

            if (app.isInstalled && !app.wasCloned) {
                View rootView = ((android.app.Activity) context).findViewById(android.R.id.content);
                View progressOverlay = rootView.findViewById(R.id.progressOverlay);
                if (progressOverlay != null) {
                    progressOverlay.setVisibility(View.VISIBLE);
                }

                new Thread(() -> {
    // returns InstallResult, not boolea
    
    com.vbox.entity.pm.InstallResult result =
            VBoxCore.get().installPackageAsUser(app.packageName, USER_ID);

    boolean success = result != null && result.success; // or compare to SUCCESS enum

    ((android.app.Activity) context).runOnUiThread(() -> {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
        if (success) {
            app.wasCloned = true;
            Toast.makeText(context,
                    context.getString(R.string.app_installed, app.name),
                    Toast.LENGTH_SHORT).show();
            holder.icon.clearColorFilter();
        } else {
            Toast.makeText(context,
                    context.getString(R.string.failed_to_clone_app, app.name),
                    Toast.LENGTH_SHORT).show();
        }
    });
}).start();
            } else {
                Toast.makeText(context, context.getString(R.string.app_not_installed_on_device, app.name), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return installedApps.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            name = itemView.findViewById(R.id.app_name);
        }
    }

    public static class AppModel {
        String name;
        String packageName;
        Drawable icon;
        boolean isInstalled;
        boolean wasCloned = false;

        public AppModel(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
        }
    }

    static class MetaCore {
        public static boolean cloneApp(String packageName) {
            try {
                VBoxCore.get().installPackageAsUser(packageName, USER_ID);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        }

    }
}