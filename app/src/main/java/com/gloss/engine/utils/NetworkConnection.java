package com.glass.engine.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.widget.Toast;

import com.blankj.molihuan.utilcode.util.ToastUtils;
import com.glass.engine.BoxApplication;


public class NetworkConnection {

    public static class CheckInternet {
        Context context;
        boolean isShow = false;

        public CheckInternet(Context ctx) {
            context = ctx;
        }


        public void registerNetworkCallback() {
            ToastUtils toast = ToastUtils.make();
            toast.setNotUseSystemToast();
            try {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                new NetworkRequest.Builder();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            isShow = false;
                            BoxApplication.get().setInternetAvailable(true);
                        }

                        @Override
                        public void onLost(Network network) {
                            BoxApplication.get().setInternetAvailable(false);
                            if (!isShow) {
                                Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
                                isShow = true;
                            }
                        }
                    });
                }
                BoxApplication.get().setInternetAvailable(false);
            } catch (Exception e) {
                BoxApplication.get().setInternetAvailable(false);
            }
        }
    }
}
