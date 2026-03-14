package com.glass.engine.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import net.lingala.zip4j.ZipFile;

public class Server extends AsyncTask<String, String, String> {

    static {
        System.loadLibrary("client");
    }

    private Context context;
    private String license;

    @SuppressLint("UseCompatLoadingForDrawables")
    public Server(Context context, String msg) {
        this.context = context;
        this.license = getLicenseFromFile();
    }

    private String getLicenseFromFile() {
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
                String license = json.getString("licence");
                return license;
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            // Получаем URL из Api класса
            String downloadUrl = Api.socklink();
            
            // Попробуем альтернативные URL если основной не работает
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                downloadUrl = "   ";
            }
            
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                android.util.Log.e("SERVER", "💥 Download URL is null or empty");
                return "ERROR_NO_URL";
            }
            
            URL url = new URL(downloadUrl);
            android.util.Log.d("SERVER", "🔗 Starting download from: " + downloadUrl);
            
            // Try different authentication methods
            if (!tryDownloadWithAuth(url, "Bearer " + license)) {
                if (!tryDownloadWithAuth(url, license)) {
                    if (!tryDownloadWithAuth(url, "Key " + license)) {
                        if (!tryDownloadWithAuth(url, "Token " + license)) {
                            if (!tryDownloadWithAuth(url, "ApiKey " + license)) {
                                if (!tryDownloadWithQueryParam(url)) {
                                    android.util.Log.e("SERVER", "💥 All authentication methods failed");
                                    return "ERROR_AUTH_FAILED";
                                }
                            }
                        }
                    }
                }
            }
            
            android.util.Log.d("SERVER", "✅ Download completed successfully");
            return "SUCCESS";
            
        } catch (Exception e) {
            android.util.Log.e("SERVER", "💥 Download error: " + e.getMessage(), e);
            return "ERROR_EXCEPTION: " + e.getMessage();
        }
    }
    
    private boolean tryDownloadWithAuth(URL url, String authValue) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Add license header for authentication
            if (license != null && !license.isEmpty()) {
                connection.setRequestProperty("Authorization", authValue);
                connection.setRequestProperty("X-License", license);
                connection.setRequestProperty("X-Key", license);
                connection.setRequestProperty("Key", license);
                connection.setRequestProperty("License", license);
                connection.setRequestProperty("api-key", license);
                connection.setRequestProperty("x-api-key", license);
                connection.setRequestProperty("token", license);
                connection.setRequestProperty("x-token", license);
            } else {
                return false;
            }
            
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Download successful
                int lenghtOfFile = connection.getContentLength();
                
                InputStream input = connection.getInputStream();
                String fileName = "assets11.zip";
                File pathBase = new File(context.getFilesDir().getPath());
                if (!pathBase.exists()) {
                    pathBase.mkdirs();
                }
                File pathOutput = new File(pathBase.toString() + "/" + fileName);
                OutputStream output = new FileOutputStream(pathOutput.toString());
                byte data[] = new byte[1024];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);
                }
                if (pathOutput.exists()) {
                    new File(pathOutput.toString()).setExecutable(true, true);
                }
                output.close();
                input.close();
                return true;
            } else {
                // Read error response to understand what the server expects
                try {
                    InputStream errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                    }
                } catch (Exception e) {
                }
                
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(String result) {
        android.util.Log.d("SERVER", "📱 onPostExecute called with result: " + result);
        
        if (result == null || result.startsWith("ERROR")) {
            android.util.Log.e("SERVER", "💥 Download failed: " + result);
            return;
        }
        
        File pathBase = new File(context.getFilesDir().getPath());
        File pathBase2 = new File(context.getFilesDir().getPath());

        File zipFile = new File(context.getFilesDir() + "/assets11.zip");
        if (zipFile.exists()) {
            android.util.Log.d("SERVER", "📦 ZIP file found, extracting...");
            boolean extractSuccess = zip4j(context.getFilesDir() + "/assets11.zip", context.getFilesDir() + "", Api.password());
            if (extractSuccess) {
                android.util.Log.d("SERVER", "✅ Extraction completed successfully");
            } else {
                android.util.Log.e("SERVER", "💥 Extraction failed");
            }
        } else {
            android.util.Log.e("SERVER", "💥 ZIP file not found after download");
        }

        File newFile = new File(pathBase.toString() + "/assets11.zip");
        if (newFile.exists()) {
            newFile.delete();
            android.util.Log.d("SERVER", "🗑️ Cleaned up ZIP file");
        }
    }

    private boolean zip4j(String path, String outpath, String password) {
        try {
            new ZipFile(path, password.toCharArray()).extractAll(outpath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        
        // Проверяем режим и настраиваем пути соответственно
        boolean isRootMode = false; //com.glass.engine.BoxApplication.get().checkRootAccess();
        
        if (isRootMode) {
            // Root режим: копируем в /data/local/tmp/
            // Поддерживаем как новые имена (delta_*), так и старые (pubg_sock/bypass)
            jknfthrbvgy("/delta_rootbypass");
            jknfthrbvgy("/delta_sock");
            jknfthrbvgy("/bypass");
            jknfthrbvgy("/pubg_sock");
            
            // НЕ запускаем bypass здесь - это будет сделано LoadingActivity после запуска игры
            
            // Устанавливаем socket путь для LoadingActivity (root режим)
            try {
                java.lang.reflect.Field socketField = com.glass.engine.activity.LoadingActivity.class.getDeclaredField("socket");
                socketField.setAccessible(true);
                socketField.set(null, "/data/local/tmp/delta_sock");
                
                java.lang.reflect.Field daemonPathField = com.glass.engine.activity.LoadingActivity.class.getDeclaredField("daemonPath");
                daemonPathField.setAccessible(true);
                daemonPathField.set(null, "/data/local/tmp/delta_sock");
                
                // Логируем установку путей для root режима
                android.util.Log.d("SERVER", "🔧 [ROOT] Set socket path to: /data/local/tmp/delta_sock");
                
            } catch (Exception e) {
                android.util.Log.e("SERVER", "💥 [ROOT] Error setting socket path: " + e.getMessage());
            }
        } else {
            // Non-root режим: файлы остаются в app directory
            
            // Устанавливаем socket путь для LoadingActivity (non-root режим)
            try {
                String appPath = context.getFilesDir() + "/delta_sock";
                java.lang.reflect.Field socketField = com.glass.engine.activity.LoadingActivity.class.getDeclaredField("socket");
                socketField.setAccessible(true);
                socketField.set(null, appPath);
                
                java.lang.reflect.Field daemonPathField = com.glass.engine.activity.LoadingActivity.class.getDeclaredField("daemonPath");
                daemonPathField.setAccessible(true);
                daemonPathField.set(null, appPath);
                
            } catch (Exception e) {
            }
        }
        
        // НЕ запускаем overlay здесь - это будет сделано LoadingActivity после запуска игры

        return true;
    }



    public void jknfthrbvgy(String path) {
        try {
            String fullPath = context.getFilesDir() + path;
            android.util.Log.d("SERVER", "🔍 Checking file: " + fullPath);
            
            File file = new File(fullPath);
            if (file.exists()) {
                android.util.Log.d("SERVER", "✅ File exists: " + path + " (size: " + file.length() + " bytes)");
                
                // Копируем файл в /data/local/tmp/ для возможности запуска
                String tmpPath = "/data/local/tmp" + path;
                copyToTmpAndChmod(fullPath, tmpPath);
                
                // Не запускаем pubg_sock здесь, он будет запущен в startOverlayHybrid()
            } else {
                android.util.Log.w("SERVER", "⚠️ File not found: " + path);
            }
        } catch (Exception e) {
            android.util.Log.e("SERVER", "💥 Error in jknfthrbvgy for " + path + ": " + e.getMessage(), e);
        }
    }
    
    private void copyToTmpAndChmod(String srcPath, String dstPath) {
        try {
            android.util.Log.d("SERVER", "🔧 Copying file: " + srcPath + " -> " + dstPath);
            
            // Используем root команды для копирования
            String copyCommand = "cp " + srcPath + " " + dstPath;
            String chmodCommand = "chmod 777 " + dstPath;
            
            // Выполняем копирование через root
            if (com.glass.engine.BoxApplication.get().checkRootAccess()) {
                
                // Копируем файл
                com.topjohnwu.superuser.Shell.Result copyResult = com.topjohnwu.superuser.Shell.su(copyCommand).exec();
                if (copyResult.isSuccess()) {
                    android.util.Log.d("SERVER", "✅ File copied successfully");
                    
                    // Устанавливаем права
                    com.topjohnwu.superuser.Shell.Result chmodResult = com.topjohnwu.superuser.Shell.su(chmodCommand).exec();
                    if (chmodResult.isSuccess()) {
                        android.util.Log.d("SERVER", "✅ Permissions set successfully");
                    } else {
                        android.util.Log.e("SERVER", "💥 Failed to set permissions: " + chmodResult.getErr());
                    }
                } else {
                    android.util.Log.e("SERVER", "💥 Failed to copy file: " + copyResult.getErr());
                }
            } else {
                android.util.Log.e("SERVER", "💥 No root access for file operations");
            }
            
        } catch (Exception e) {
            android.util.Log.e("SERVER", "💥 Error in copyToTmpAndChmod: " + e.getMessage(), e);
        }
    }
    
    private void startCyclicBypass() {
        try {
            final long startTime = System.currentTimeMillis();
            final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            final boolean isRootMode = false; //com.glass.engine.BoxApplication.get().checkRootAccess();
            
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() - startTime >= 40_000) {
                        return;
                    }

                    
                    if (isRootMode) {
                        // Root режим: используем /data/local/tmp/
                        com.glass.engine.BoxApplication.get().doExe("/data/local/tmp/bypass 1");

                    } else {
                        // Non-root режим: используем app directory
                        String bypassPath = context.getFilesDir() + "/bypass";
                        try {
                            Runtime.getRuntime().exec(bypassPath + " 1");
                           // Runtime.getRuntime().exec(bypassPath + " 2");
                        } catch (Exception e) {
                        }
                    }

                    handler.postDelayed(this, 2000);
                }
            };

            handler.postDelayed(runnable, 2000);
        } catch (Exception e) {
        }
    }
    
    

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) for (File child : fileOrDirectory.listFiles())
            deleteRecursive(child);

        fileOrDirectory.delete();
    }

    private boolean tryDownloadWithQueryParam(URL url) {
        try {
            // Try with license as query parameter
            String urlWithKey = url.toString() + "?key=" + license;
            
            URL newUrl = new URL(urlWithKey);
            HttpURLConnection connection = (HttpURLConnection) newUrl.openConnection();
            
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Download successful
                int lenghtOfFile = connection.getContentLength();
                
                InputStream input = connection.getInputStream();
                String fileName = "assets11.zip";
                File pathBase = new File(context.getFilesDir().getPath());
                if (!pathBase.exists()) {
                    pathBase.mkdirs();
                }
                File pathOutput = new File(pathBase.toString() + "/" + fileName);
                OutputStream output = new FileOutputStream(pathOutput.toString());
                byte data[] = new byte[1024];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);
                }
                
                if (pathOutput.exists()) {
                    new File(pathOutput.toString()).setExecutable(true, true);
                }
                output.close();
                input.close();
                return true;
            } else {
                
                // Read error response
                try {
                    InputStream errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(errorStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                    }
                } catch (Exception e) {
                }
                
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
