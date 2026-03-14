#include <jni.h>
#include <string>
#include <fstream>
#include <ctime>
#include <iomanip>
#include <algorithm>
#include <vector>
#include <sstream>
#include <random>
#include <memory>
#include <thread>
#include <chrono>

#include <android/log.h>
#include <dlfcn.h>
#include <sys/system_properties.h>

#include "json.hpp"
#include "obfuscate.h"
#include "LOGIN/login.h"
#include <jni.h>
#include <string>
#include <android/log.h>
#include "ESP.h"
#include "Hacks.h"

ESP espOverlay;

int type = 1;



extern "C" JNIEXPORT jboolean JNICALL
Java_com_glass_engine_activity_MainActivity_getesp(JNIEnv *env, jobject thiz) {
if(!g_Token.empty() && !g_Auth.empty() && g_Token == g_Auth){

        return JNI_TRUE;
    }else{
    exit(-1);
    return JNI_FALSE;
    }
}
// Методы для BoxApplication
extern "C" JNIEXPORT void JNICALL
Java_com_glass_engine_fragments_LoginFragment_setEspAllEnabled(JNIEnv *, jclass, jboolean enabled) {
    espJAVA = enabled;
}
extern "C" JNIEXPORT void JNICALL
Java_com_glass_engine_fragments_LoginFragment_setAimAllEnabled(JNIEnv *, jclass, jboolean enabled) {
    aimJAVA = enabled;
}
extern "C" JNIEXPORT void JNICALL
Java_com_glass_engine_fragments_LoginFragment_setBulletAllEnabled(JNIEnv *, jclass, jboolean enabled) {
    bulletJAVA = enabled;
}

// Методы для MainActivity
extern "C" JNIEXPORT void JNICALL
Java_com_glass_engine_activity_MainActivity_setEspAllEnabled(JNIEnv *, jclass, jboolean enabled) {
    espJAVA = enabled;
}
extern "C" JNIEXPORT void JNICALL
Java_com_glass_engine_activity_MainActivity_setAimAllEnabled(JNIEnv *, jclass, jboolean enabled) {
    aimJAVA = enabled;
}
extern "C" JNIEXPORT void JNICALL
Java_com_glass_engine_activity_MainActivity_setBulletAllEnabled(JNIEnv *, jclass, jboolean enabled) {
    bulletJAVA = enabled;
}


using json = nlohmann::json;

// Глобальная переменная для отслеживания статуса логина
static bool g_loginValid = false;

// Вспомогательная функция для получения пути к cacert.pem через JNI
std::string GetPrivateCacertPath(JNIEnv* env, jobject context) {
    // Получаем ClassLoader из context
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getClassLoader = env->GetMethodID(contextClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
    jobject classLoader = env->CallObjectMethod(context, getClassLoader);
    jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
    jmethodID loadClass = env->GetMethodID(classLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    jstring strClassName = env->NewStringUTF("com.glass.engine.BoxApplication");
    jclass bridgeClass = (jclass)env->CallObjectMethod(classLoader, loadClass, strClassName);
    env->DeleteLocalRef(strClassName);
    // Теперь получаем метод
    jmethodID copyCacertMethod = env->GetStaticMethodID(bridgeClass, "copyCacertFromAssetsIfNeeded", "(Landroid/content/Context;)Ljava/lang/String;");
    jstring jCacertPath = (jstring)env->CallStaticObjectMethod(bridgeClass, copyCacertMethod, context);
    const char* cacertPath = env->GetStringUTFChars(jCacertPath, nullptr);
    std::string result = cacertPath;
    env->ReleaseStringUTFChars(jCacertPath, cacertPath);
    env->DeleteLocalRef(jCacertPath);
    return result;
}

// JNI функции (совместимость со старым интерфейсом)
extern "C" JNIEXPORT jstring JNICALL
Java_com_glass_engine_activity_LoginActivity_GetKey(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(OBFUSCATE("https://t.me/Glass_Engine"));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_glass_engine_BoxApplication_ApiKeyBox(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(OBFUSCATE("com.glass.engine"));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_glass_engine_activity_MainActivity_exdate(JNIEnv *env, jclass clazz) {
    if (bValid && !EXP.empty()) {
        return env->NewStringUTF(EXP.c_str());
    } else {
        return env->NewStringUTF("INVALID");
    }
}


// Дополнительные JNI функции для совместимости
extern "C"
JNIEXPORT jstring JNICALL
Java_com_glass_engine_component_Api_password(JNIEnv *env, jclass thiz) {
    return env->NewStringUTF(OBFUSCATE("qwertyzip"));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_glass_engine_component_Api_socklink(JNIEnv *env, jclass thiz) {
    return env->NewStringUTF(OBFUSCATE("https://keypanel.tech/sock/pubg1.zip"));
}

// Delta Force assets functions
extern "C"
JNIEXPORT jstring JNICALL
Java_com_glass_engine_activity_LoadingActivity_getDeltaForceUrl(JNIEnv *env, jclass thiz) {
    return env->NewStringUTF(OBFUSCATE("https://keypanel.tech/sock/delta.zip"));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_glass_engine_activity_LoadingActivity_getDeltaForcePassword(JNIEnv *env, jclass thiz) {
    return env->NewStringUTF(OBFUSCATE("qwertyzip"));
}

// PUBG assets functions
extern "C"
JNIEXPORT jstring JNICALL
Java_com_glass_engine_activity_LoadingActivity_getPubgUrl(JNIEnv *env, jclass thiz) {
    return env->NewStringUTF(OBFUSCATE("https://keypanel.tech/sock/pubg1.zip"));
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_glass_engine_activity_LoadingActivity_getPubgPassword(JNIEnv *env, jclass thiz) {
    return env->NewStringUTF(OBFUSCATE("qwertyzip"));
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_glass_engine_activity_LoadingActivity_getPubgFileNames(JNIEnv *env, jclass thiz) {
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(3, stringClass, nullptr);
    env->SetObjectArrayElement(array, 0, env->NewStringUTF(OBFUSCATE("pubg_bypass")));
    env->SetObjectArrayElement(array, 1, env->NewStringUTF(OBFUSCATE("pubg_rootbypass")));
    env->SetObjectArrayElement(array, 2, env->NewStringUTF(OBFUSCATE("pubg_sock")));
    return array;
}


extern "C"
JNIEXPORT jboolean JNICALL
Java_com_glass_engine_activity_MainActivity_ich(JNIEnv* env, jclass clazz) {
    return (!g_Token.empty() &&
            !g_Auth.empty() &&
            g_Token == g_Auth) ? JNI_TRUE : JNI_FALSE;
}

/*
bool obscureLogic(bool condition) {
    return condition ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_glass_engine_activity_MainActivity_fucku(JNIEnv *env, jclass clazz) {
    return obscureLogic(false);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_glass_engine_activity_MainActivity_noob(JNIEnv *env, jclass clazz) {
    return obscureLogic(g_loginValid);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_glass_engine_activity_MainActivity_xxx(JNIEnv *env, jclass clazz) {
    return obscureLogic(g_loginValid);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_glass_engine_activity_MainActivity_lund(JNIEnv *env, jclass clazz) {
    return obscureLogic(g_loginValid);
}
*/
// Глобальные переменные для чит-режимов
bool isEspAll = true;
bool isAimAll = true;
bool isBulletAll = true;

extern "C" {
// JNI функция для установки режима чита
JNIEXPORT void JNICALL
Java_com_glass_engine_utils_NativeBridge_setCheatMode(JNIEnv *env, jclass clazz, jint mode) {
    // mode: 0 = basic, 1 = advanced, 2 = ultimate
    if (mode == 0) { // basic
        isEspAll = true;
        isAimAll = false;
        isBulletAll = false;
    } else if (mode == 1) { // advanced
        isEspAll = true;
        isAimAll = true;
        isBulletAll = false;
    } else if (mode == 2) { // ultimate
        isEspAll = true;
        isAimAll = false;
        isBulletAll = true;
    }
}
}


