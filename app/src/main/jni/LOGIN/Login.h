#ifndef LOGIN_H
#define LOGIN_H

#pragma once
#include <jni.h>
#include <string>
#include <vector>
#include <openssl/x509.h>
#include <curl/curl.h>
#include <string>
#include <fstream>
#include <cstring>
#include <jni.h>
#include <sys/system_properties.h>
#include <curl/curl.h>
#include <openssl/evp.h>
#include <openssl/pem.h>
#include <openssl/rsa.h>
#include <openssl/err.h>
#include <openssl/md5.h>
#include "StrEnc.h"
#include "json.hpp"
#include "oxorany/oxorany.h"
std::string g_Token, g_Auth;
bool bValid = false;
std::string errorMsg;
using json = nlohmann::json;
static bool isLogin = false;
bool b_Fps = false;
char logkey [64];
std::string EXP = "";
const char *GetAndroidID(JNIEnv *env, jobject context) {
    jclass contextClass = env->FindClass(/*android/content/Context*/ StrEnc("`L+&0^[S+-:J^$,r9q92(as", "\x01\x22\x4F\x54\x5F\x37\x3F\x7C\x48\x42\x54\x3E\x3B\x4A\x58\x5D\x7A\x1E\x57\x46\x4D\x19\x07", 23).c_str());
    jmethodID getContentResolverMethod = env->GetMethodID(contextClass, /*getContentResolver*/ StrEnc("E8X\\7r7ys_Q%JS+L+~", "\x22\x5D\x2C\x1F\x58\x1C\x43\x1C\x1D\x2B\x03\x40\x39\x3C\x47\x3A\x4E\x0C", 18).c_str(), /*()Landroid/content/ContentResolver;*/ StrEnc("8^QKmj< }5D:9q7f.BXkef]A*GYLNg}B!/L", "\x10\x77\x1D\x2A\x03\x0E\x4E\x4F\x14\x51\x6B\x59\x56\x1F\x43\x03\x40\x36\x77\x28\x0A\x08\x29\x24\x44\x33\x0B\x29\x3D\x08\x11\x34\x44\x5D\x77", 35).c_str());
    jclass settingSecureClass = env->FindClass(/*android/provider/Settings$Secure*/ StrEnc("T1yw^BCF^af&dB_@Raf}\\FS,zT~L(3Z\"", "\x35\x5F\x1D\x05\x31\x2B\x27\x69\x2E\x13\x09\x50\x0D\x26\x3A\x32\x7D\x32\x03\x09\x28\x2F\x3D\x4B\x09\x70\x2D\x29\x4B\x46\x28\x47", 32).c_str());
    jmethodID getStringMethod = env->GetStaticMethodID(settingSecureClass, /*getString*/ StrEnc("e<F*J5c0Y", "\x02\x59\x32\x79\x3E\x47\x0A\x5E\x3E", 9).c_str(), /*(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;*/ StrEnc("$6*%R*!XO\"m18o,0S!*`uI$IW)l_/_knSdlRiO1T`2sH|Ouy__^}%Y)JsQ:-\"(2_^-$i{?H", "\x0C\x7A\x4B\x4B\x36\x58\x4E\x31\x2B\x0D\x0E\x5E\x56\x1B\x49\x5E\x27\x0E\x69\x0F\x1B\x3D\x41\x27\x23\x7B\x09\x2C\x40\x33\x1D\x0B\x21\x5F\x20\x38\x08\x39\x50\x7B\x0C\x53\x1D\x2F\x53\x1C\x01\x0B\x36\x31\x39\x46\x0C\x15\x43\x2B\x05\x30\x15\x41\x43\x46\x55\x70\x0D\x59\x56\x00\x15\x58\x73", 71).c_str());

    auto obj = env->CallObjectMethod(context, getContentResolverMethod);
    auto str = (jstring) env->CallStaticObjectMethod(settingSecureClass, getStringMethod, obj, env->NewStringUTF(/*android_id*/ StrEnc("ujHO)8OfOE", "\x14\x04\x2C\x3D\x46\x51\x2B\x39\x26\x21", 10).c_str()));
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceModel(JNIEnv *env) {
    jclass buildClass = env->FindClass(/*android/os/Build*/ StrEnc("m5I{GKGWBP-VOxkA", "\x0C\x5B\x2D\x09\x28\x22\x23\x78\x2D\x23\x02\x14\x3A\x11\x07\x25", 16).c_str());
    jfieldID modelId = env->GetStaticFieldID(buildClass, /*MODEL*/ StrEnc("|}[q:", "\x31\x32\x1F\x34\x76", 5).c_str(), /*Ljava/lang/String;*/ StrEnc(".D:C:ETZ1O-Ib&^h.Y", "\x62\x2E\x5B\x35\x5B\x6A\x38\x3B\x5F\x28\x02\x1A\x16\x54\x37\x06\x49\x62", 18).c_str());

    auto str = (jstring) env->GetStaticObjectField(buildClass, modelId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceBrand(JNIEnv *env) {
    jclass buildClass = env->FindClass(/*android/os/Build*/ StrEnc("0iW=2^>0zTRB!B90", "\x51\x07\x33\x4F\x5D\x37\x5A\x1F\x15\x27\x7D\x00\x54\x2B\x55\x54", 16).c_str());
    jfieldID modelId = env->GetStaticFieldID(buildClass, /*BRAND*/ StrEnc("@{[FP", "\x02\x29\x1A\x08\x14", 5).c_str(), /*Ljava/lang/String;*/ StrEnc(".D:C:ETZ1O-Ib&^h.Y", "\x62\x2E\x5B\x35\x5B\x6A\x38\x3B\x5F\x28\x02\x1A\x16\x54\x37\x06\x49\x62", 18).c_str());

    auto str = (jstring) env->GetStaticObjectField(buildClass, modelId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetPackageName(JNIEnv *env, jobject context) {
    jclass contextClass = env->FindClass(/*android/content/Context*/ StrEnc("`L+&0^[S+-:J^$,r9q92(as", "\x01\x22\x4F\x54\x5F\x37\x3F\x7C\x48\x42\x54\x3E\x3B\x4A\x58\x5D\x7A\x1E\x57\x46\x4D\x19\x07", 23).c_str());
    jmethodID getPackageNameId = env->GetMethodID(contextClass, /*getPackageName*/ StrEnc("YN4DaP)!{wRGN}", "\x3E\x2B\x40\x14\x00\x33\x42\x40\x1C\x12\x1C\x26\x23\x18", 14).c_str(), /*()Ljava/lang/String;*/ StrEnc("VnpibEspM(b]<s#[9cQD", "\x7E\x47\x3C\x03\x03\x33\x12\x5F\x21\x49\x0C\x3A\x13\x20\x57\x29\x50\x0D\x36\x7F", 20).c_str());

    auto str = (jstring) env->CallObjectMethod(context, getPackageNameId);
    return env->GetStringUTFChars(str, 0);
}

const char *GetDeviceUniqueIdentifier(JNIEnv *env, const char *uuid) {
    jclass uuidClass = env->FindClass(/*java/util/UUID*/ StrEnc("B/TxJ=3BZ_]SFx", "\x28\x4E\x22\x19\x65\x48\x47\x2B\x36\x70\x08\x06\x0F\x3C", 14).c_str());

    auto len = strlen(uuid);

    jbyteArray myJByteArray = env->NewByteArray(len);
    env->SetByteArrayRegion(myJByteArray, 0, len, (jbyte *) uuid);

    jmethodID nameUUIDFromBytesMethod = env->GetStaticMethodID(uuidClass, /*nameUUIDFromBytes*/ StrEnc("P6LV|'0#A+zQmoat,", "\x3E\x57\x21\x33\x29\x72\x79\x67\x07\x59\x15\x3C\x2F\x16\x15\x11\x5F", 17).c_str(), /*([B)Ljava/util/UUID;*/ StrEnc("sW[\"Q[W3,7@H.vT0) xB", "\x5B\x0C\x19\x0B\x1D\x31\x36\x45\x4D\x18\x35\x3C\x47\x1A\x7B\x65\x7C\x69\x3C\x79", 20).c_str());
    jmethodID toStringMethod = env->GetMethodID(uuidClass, /*toString*/ StrEnc("2~5292eW", "\x46\x11\x66\x46\x4B\x5B\x0B\x30", 8).c_str(), /*()Ljava/lang/String;*/ StrEnc("P$BMc' #j?<:myTh_*h0", "\x78\x0D\x0E\x27\x02\x51\x41\x0C\x06\x5E\x52\x5D\x42\x2A\x20\x1A\x36\x44\x0F\x0B", 20).c_str());

    auto obj = env->CallStaticObjectMethod(uuidClass, nameUUIDFromBytesMethod, myJByteArray);
    auto str = (jstring) env->CallObjectMethod(obj, toStringMethod);
    return env->GetStringUTFChars(str, 0);
}

#include <curl/curl.h>

struct MemoryStruct {
    char *memory;
    size_t size;
};

static size_t WriteMemoryCallback(void *contents, size_t size, size_t nmemb, void *userp) {
    size_t realsize = size * nmemb;
    struct MemoryStruct *mem = (struct MemoryStruct *) userp;

    mem->memory = (char *) realloc(mem->memory, mem->size + realsize + 1);
    if (mem->memory == NULL) {
        return 0;
    }

    memcpy(&(mem->memory[mem->size]), contents, realsize);
    mem->size += realsize;
    mem->memory[mem->size] = 0;

    return realsize;
}

inline std::string xor_encrypt(const std::string& data, const std::string& key) {
    std::string result;
    result.reserve(data.size());

    for (size_t i = 0; i < data.size(); ++i) {
        result += data[i] ^ key[i % key.length()];
    }

    return result;
}

inline std::string xor_decrypt(const std::string& data, const std::string& key) {
    return xor_encrypt(data, key);
}

inline std::string base64_encode(const std::string& data) {
    static const char* chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string result;
    size_t i = 0;
    unsigned char a3[3];
    unsigned char a4[4];

    while (data.length() - i >= 3) {
        a3[0] = static_cast<unsigned char>(data[i]);
        a3[1] = static_cast<unsigned char>(data[i + 1]);
        a3[2] = static_cast<unsigned char>(data[i + 2]);

        a4[0] = (a3[0] & 0xfc) >> 2;
        a4[1] = ((a3[0] & 0x03) << 4) + ((a3[1] & 0xf0) >> 4);
        a4[2] = ((a3[1] & 0x0f) << 2) + ((a3[2] & 0xc0) >> 6);
        a4[3] = a3[2] & 0x3f;

        for (int j = 0; j < 4; j++)
            result += chars[a4[j]];

        i += 3;
    }

    if (data.length() - i > 0) {
        int remaining = data.length() - i;
        for (int j = 0; j < 3; j++)
            a3[j] = (j < remaining) ? static_cast<unsigned char>(data[i + j]) : 0;

        a4[0] = (a3[0] & 0xfc) >> 2;
        a4[1] = ((a3[0] & 0x03) << 4) + ((a3[1] & 0xf0) >> 4);
        a4[2] = ((a3[1] & 0x0f) << 2) + ((a3[2] & 0xc0) >> 6);
        a4[3] = a3[2] & 0x3f;

        for (int j = 0; j < remaining + 1; j++)
            result += chars[a4[j]];
        while (result.length() % 4)
            result += '=';
    }

    return result;
}

inline std::string base64_decode(const std::string& input) {
    static const std::string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    std::string result;
    std::vector<int> T(256, -1);
    for (int i = 0; i < 64; i++)
        T[static_cast<unsigned char>(chars[i])] = i;

    int val = 0, valb = -8;
    for (unsigned char c : input) {
        if (T[c] == -1) break;
        val = (val << 6) + T[c];
        valb += 6;
        if (valb >= 0) {
            result.push_back(char((val >> valb) & 0xFF));
            valb -= 8;
        }
    }

    return result;
}


std::string encryptData(const std::string& data, const std::string& key) {
    std::string encrypted = xor_encrypt(data, key);
    std::string encoded = base64_encode(encrypted);
    return "{\"data\":\"" + encoded + "\"}";
}

std::string decryptData(const std::string& encryptedData, const std::string& key) {
    try {
        auto jsonObj = nlohmann::json::parse(encryptedData);

        if (!jsonObj.contains("data") || !jsonObj["data"].is_string())
            return "";

        std::string encoded = jsonObj["data"].get<std::string>();
        std::string decoded = base64_decode(encoded);
        return xor_decrypt(decoded, key);
    } catch (...) {
        return "";
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_glass_engine_fragments_LoginFragment_Check(JNIEnv *env, jclass clazz, jobject mContext, jstring mUserKey) {
    auto userKey = env->GetStringUTFChars(mUserKey, 0);
    
    // encryption key (same as original)
    const std::string encryption_key = oxorany("JiM21rNU12eERlNmpqa3FuQks");

    // Gather device identifiers (use provided helpers)
    std::string androidId = GetAndroidID(env, mContext);
    std::string deviceModel = GetDeviceModel(env);
    std::string deviceBrand = GetDeviceBrand(env);

    std::string hwid = androidId + deviceModel + deviceBrand;
    std::string UUID = GetDeviceUniqueIdentifier(env, hwid.c_str());

    std::string version = oxorany("1.0");
    std::string gametype = oxorany("PUBG");

    std::string errorMsg; // returned on failure

    // Prepare memory chunk for curl write callback
    struct MemoryStruct chunk{};
    chunk.memory = (char*) malloc(1);
    chunk.size = 0;

    CURL* curl = curl_easy_init();
    if (!curl) {
        free(chunk.memory);
        return env->NewStringUTF("Could not initialize curl");
    }

    // Build payload JSON
    try {
        nlohmann::json payload = {
            {"license_key", userKey},
            {"hwid", UUID},
            {"game_type", gametype},
            {"version", version}
        };

        std::string jsonPayload = payload.dump();
        std::string encryptedPayload = encryptData(jsonPayload, encryption_key);

        // license server URL (base64 decoded like original)
        std::string licenseServerUrl = base64_decode("aHR0cHM6Ly94cHJvamVjdHBhbmVsLmNvbS9hdXRoL2FwaS8yLjA=");

        curl_easy_setopt(curl, CURLOPT_URL, licenseServerUrl.c_str());
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
        curl_easy_setopt(curl, CURLOPT_POST, 1L);

        struct curl_slist* headers = NULL;
        headers = curl_slist_append(headers, "Content-Type: application/json");
        curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

        // POST body (encrypted)
        curl_easy_setopt(curl, CURLOPT_POSTFIELDS, encryptedPayload.c_str());
        curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, (long)encryptedPayload.length());

        // write callback
        curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, WriteMemoryCallback);
        curl_easy_setopt(curl, CURLOPT_WRITEDATA, (void*)&chunk);

        // disable peer verification like original (careful in production)
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);

        // perform
        CURLcode res = curl_easy_perform(curl);
        if (res != CURLE_OK) {
            errorMsg = curl_easy_strerror(res);
        } else {
            // got response in chunk.memory
            std::string decryptedResponse;
            try {
                decryptedResponse = decryptData(chunk.memory, encryption_key);
            } catch (...) {
                decryptedResponse.clear();
            }

            if (decryptedResponse.empty()) {
                errorMsg = "Failed to decrypt server response";
            } else {
                try {
                    nlohmann::json response = nlohmann::json::parse(decryptedResponse);

                    if (response.contains("status") && response["status"] == "success") {
                        auto data = response["data"];

                        std::string expiryDate;
                        std::string serverVersion;
                        try { expiryDate = data.value("expiry_date", ""); } catch(...) { expiryDate = ""; }
                        try { serverVersion = data.value("version", ""); } catch(...) { serverVersion = ""; }

                        // convert strings to ints if needed (keep original behavior)
                        if (data.contains("max_devices") && data["max_devices"].is_string()) {
                            try { data["max_devices"] = std::stoi(data["max_devices"].get<std::string>()); } catch(...) {}
                        }
                        if (data.contains("active_devices") && data["active_devices"].is_string()) {
                            try { data["active_devices"] = std::stoi(data["active_devices"].get<std::string>()); } catch(...) {}
                        }

                        if (serverVersion != version) {
                            errorMsg = "Your version is old, please update it. Current version: " + serverVersion;
                        } else {
                            // success path: set globals like original
                            EXP = expiryDate;
                            g_Token = oxorany("0wQRlDkgoQlf");
                            g_Auth  = oxorany("0wQRlDkgoQlf");
                            b_Fps = true;
                            bValid = (g_Token == g_Auth);
                         //   pthread(__1__);
                            // cleanup before returning OK
                            curl_slist_free_all(headers);
                            curl_easy_cleanup(curl);
                            if (chunk.memory) {
                                free(chunk.memory);
                            }

                            return env->NewStringUTF("OK");
                        }
                    } else {
                        // non-success: try to read message
                        if (response.contains("message") && response["message"].is_string()) {
                            errorMsg = response["message"].get<std::string>();
                        } else {
                            errorMsg = "Server returned failure";
                        }
                    }
                } catch (nlohmann::json::exception& e) {
                    errorMsg = std::string("JSON parsing error: ") + e.what() +
                               "\nResponse: " + (chunk.memory ? chunk.memory : "");
                }
            }
        }

        // cleanup
        curl_slist_free_all(headers);
        curl_easy_cleanup(curl);
    } catch (std::exception& ex) {
        // catch any std exception during payload preparation / encrypt
        errorMsg = std::string("Exception: ") + ex.what();
        curl_easy_cleanup(curl);
    }

    if (chunk.memory) free(chunk.memory);
    return env->NewStringUTF(errorMsg.c_str());
}
#endif
