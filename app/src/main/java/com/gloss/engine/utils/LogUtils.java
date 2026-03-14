package com.glass.engine.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogUtils {
    private static final String TAG = "LogUtils";
    private static final String LOG_FILENAME = "xloader_debug.log";
    private static final String LOG_DIR = "XLoaderLogs";
    private static File logFile;
    private static FileWriter fileWriter;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static boolean loginLoggingEnabled = false; // Отключаем логи логина по умолчанию
    // === Global switch to completely disable file logging (and related Logcat) ===
    private static final boolean DISABLE_FILE_LOGGING = true;
    
    public static void initLogging(Context context) {
        if (DISABLE_FILE_LOGGING) {
            return; // no-op
        }
        try {
            // Создаем директорию для логов в папке Downloads
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File logDir = new File(downloadsDir, LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // Создаем файл лога
            logFile = new File(logDir, LOG_FILENAME);
            
            // Открываем FileWriter для записи
            fileWriter = new FileWriter(logFile, true); // true для append режима
            
            // Записываем заголовок новой сессии
            writeLog("=== XLOADER DEBUG SESSION STARTED ===");
            writeLog("Device: " + android.os.Build.MODEL);
            writeLog("Android Version: " + android.os.Build.VERSION.RELEASE);
            writeLog("SDK Level: " + android.os.Build.VERSION.SDK_INT);
            writeLog("Root Available: " + isRootAvailable());
            writeLog("=====================================");
            
            Log.d(TAG, "Logging initialized: " + logFile.getAbsolutePath());
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize logging: " + e.getMessage());
        }
    }
    
    public static void writeLog(String message) {
        writeLog(message, "INFO");
    }
    
    public static void writeLog(String message, String level) {
        if (DISABLE_FILE_LOGGING) {
            return; // no-op
        }
        // Фильтруем логи логина если они отключены
        if (!loginLoggingEnabled && isLoginRelatedLog(message)) {
            return;
        }
        
        if (fileWriter == null) {
            Log.w(TAG, "Logging not initialized, message: " + message);
            return;
        }
        
        try {
            String timestamp = dateFormat.format(new Date());
            String logEntry = String.format("[%s] [%s] %s\n", timestamp, level, message);
            
            fileWriter.write(logEntry);
            fileWriter.flush(); // Принудительно записываем на диск
            
            // Также выводим в logcat для отладки
            Log.d(TAG, "FileLog: " + message);
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to write to log file: " + e.getMessage());
        }
    }
    
    public static void writeError(String message, Throwable throwable) {
        writeLog("ERROR: " + message, "ERROR");
        if (throwable != null) {
            writeLog("Exception: " + throwable.toString(), "ERROR");
            for (StackTraceElement element : throwable.getStackTrace()) {
                writeLog("  at " + element.toString(), "ERROR");
            }
        }
    }
    
    public static void writeMetaLaunchLog(String packageName, boolean success, String details) {
        writeLog("=== META LAUNCH ATTEMPT ===");
        writeLog("Package: " + packageName);
        writeLog("Success: " + success);
        writeLog("Details: " + details);
        writeLog("===========================");
    }
    
    public static void writeTriggerLog(String triggerType, String logLine, boolean triggered) {
        writeLog("=== TRIGGER ATTEMPT ===");
        writeLog("Type: " + triggerType);
        writeLog("Log Line: " + logLine);
        writeLog("Triggered: " + triggered);
        writeLog("=======================");
    }
    
    public static void writeBypassLog(String action, String details) {
        writeLog("=== BYPASS ACTION ===");
        writeLog("Action: " + action);
        writeLog("Details: " + details);
        writeLog("====================");
    }
    
    public static void writeGameLaunchLog(String packageName, boolean success, String method) {
        writeLog("=== GAME LAUNCH ===");
        writeLog("Package: " + packageName);
        writeLog("Method: " + method);
        writeLog("Success: " + success);
        writeLog("===================");
    }
    
    public static void writeServiceLog(String serviceName, String action, boolean success) {
        writeLog("=== SERVICE LOG ===");
        writeLog("Service: " + serviceName);
        writeLog("Action: " + action);
        writeLog("Success: " + success);
        writeLog("===================");
    }
    
    public static void writePermissionLog(String permission, boolean granted) {
        writeLog("=== PERMISSION CHECK ===");
        writeLog("Permission: " + permission);
        writeLog("Granted: " + granted);
        writeLog("=======================");
    }
    
    public static void writeDeviceInfo() {
        writeLog("=== DEVICE INFO ===");
        writeLog("Manufacturer: " + android.os.Build.MANUFACTURER);
        writeLog("Model: " + android.os.Build.MODEL);
        writeLog("Product: " + android.os.Build.PRODUCT);
        writeLog("Device: " + android.os.Build.DEVICE);
        writeLog("Brand: " + android.os.Build.BRAND);
        writeLog("Hardware: " + android.os.Build.HARDWARE);
        writeLog("Fingerprint: " + android.os.Build.FINGERPRINT);
        writeLog("==================");
    }
    
    public static void writeSystemInfo() {
        writeLog("=== SYSTEM INFO ===");
        writeLog("Available Memory: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
        writeLog("Total Memory: " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB");
        writeLog("Free Memory: " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + " MB");
        writeLog("==================");
    }
    
    private static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            process.destroy();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static void closeLogging() {
        if (DISABLE_FILE_LOGGING) {
            return; // no-op
        }
        if (fileWriter != null) {
            try {
                writeLog("=== SESSION ENDED ===");
                fileWriter.close();
                fileWriter = null;
            } catch (IOException e) {
                Log.e(TAG, "Failed to close log file: " + e.getMessage());
            }
        }
    }
    
    public static String getLogFilePath() {
        if (DISABLE_FILE_LOGGING) {
            return "Disabled";
        }
        return logFile != null ? logFile.getAbsolutePath() : "Not initialized";
    }

    // Метод для проверки является ли лог связанным с логином
    private static boolean isLoginRelatedLog(String message) {
        if (message == null) return false;
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("login") || 
               lowerMessage.contains("auth") || 
               lowerMessage.contains("user") || 
               lowerMessage.contains("key") || 
               lowerMessage.contains("license") ||
               lowerMessage.contains("checklicense") ||
               lowerMessage.contains("nativechecklicense");
    }
    
    // Метод для включения/отключения логов логина
    public static void setLoginLoggingEnabled(boolean enabled) {
        loginLoggingEnabled = enabled;
    }
} 