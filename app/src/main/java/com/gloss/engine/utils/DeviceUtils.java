package com.glass.engine.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Утилитный класс для определения типа устройства
 */
public class DeviceUtils {
    
    /**
     * Определяет, является ли устройство планшетом
     * @param context Контекст приложения
     * @return true если устройство является планшетом, false если телефон
     */
    public static boolean isTablet(Context context) {
        if (context == null) {
            Log.d("DeviceUtils", "isTablet: context is null");
            return false;
        }
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = displayMetrics.widthPixels / displayMetrics.density;
        float screenHeightDp = displayMetrics.heightPixels / displayMetrics.density;
        float smallestWidthDp = Math.min(screenWidthDp, screenHeightDp);
        boolean result = smallestWidthDp >= 600;
        Log.d("DeviceUtils", "isTablet: screenWidthDp=" + screenWidthDp + ", screenHeightDp=" + screenHeightDp + ", smallestWidthDp=" + smallestWidthDp + ", result=" + result);
        return result;
    }
    
    /**
     * Определяет, является ли устройство телефоном
     * @param context Контекст приложения
     * @return true если устройство является телефоном, false если планшет
     */
    public static boolean isPhone(Context context) {
        return !isTablet(context);
    }
} 