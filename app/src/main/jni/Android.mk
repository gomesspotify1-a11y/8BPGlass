LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := client

LOCAL_CFLAGS := -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w
LOCAL_CFLAGS += -fno-rtti -fno-exceptions -fpermissive
LOCAL_CPPFLAGS += -Wno-error=format-security -fvisibility=hidden -ffunction-sections -fdata-sections -w -Werror -s -std=c++17
LOCAL_CPPFLAGS += -Wno-error=c++11-narrowing -fms-extensions -fno-rtti -fno-exceptions -fpermissive
LOCAL_LDFLAGS += -Wl,--gc-sections,--strip-all, -llog
LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES := $(LOCAL_PATH)/
LOCAL_C_INCLUDES += $(LOCAL_PATH)/includes
LOCAL_C_INCLUDES += $(LOCAL_PATH)/includes/curl/curl-android-$(TARGET_ARCH_ABI)/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/includes/curl/openssl-android-$(TARGET_ARCH_ABI)/include
LOCAL_C_INCLUDES += $(ANDROID_NDK)/sources/cxx-stl/llvm-libc++/include
LOCAL_C_INCLUDES += $(ANDROID_NDK)/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include
LOCAL_C_INCLUDES += $(ANDROID_NDK)/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/include/$(TARGET_ARCH_ABI)
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/LOGIN
LOCAL_C_INCLUDES += $(LOCAL_PATH)/LOGIN/cpr
LOCAL_C_INCLUDES += $(LOCAL_PATH)/LOGIN/Oxorany
LOCAL_C_INCLUDES += $(LOCAL_PATH)/LOGIN/openssl
	
CLIENT_SRC_FILES := $(wildcard $(LOCAL_PATH)/*.cpp)
CPR_SRC_FILES := $(wildcard $(LOCAL_PATH)/cpr/*.cpp)

LOGIN_SRC_FILES := $(wildcard $(LOCAL_PATH)/LOGIN/*.cpp)
LOGIN_CPR_SRC_FILES := $(wildcard $(LOCAL_PATH)/LOGIN/cpr/*.cpp)
LOGIN_OXORANY_SRC_FILES := $(wildcard $(LOCAL_PATH)/LOGIN/Oxorany/*.cpp)

LOCAL_SRC_FILES := $(CLIENT_SRC_FILES:$(LOCAL_PATH)/%=%)
LOCAL_SRC_FILES += $(CPR_SRC_FILES:$(LOCAL_PATH)/%=%)
LOCAL_SRC_FILES += $(LOGIN_SRC_FILES:$(LOCAL_PATH)/%=%)
LOCAL_SRC_FILES += $(LOGIN_CPR_SRC_FILES:$(LOCAL_PATH)/%=%)
LOCAL_SRC_FILES += $(LOGIN_OXORANY_SRC_FILES:$(LOCAL_PATH)/%=%)

LOCAL_CPP_FEATURES := exceptions
LOCAL_LDLIBS    := -llog -landroid -lz

LOCAL_STATIC_LIBRARIES := libcurl libssl libcrypto libnghttp2

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libcurl
LOCAL_SRC_FILES := LOGIN/lib/arm64-v8a/libcurl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libssl
LOCAL_SRC_FILES := LOGIN/lib/arm64-v8a/libssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libcrypto
LOCAL_SRC_FILES := LOGIN/lib/arm64-v8a/libcrypto.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := libnghttp2
LOCAL_SRC_FILES := LOGIN/lib/arm64-v8a/libnghttp2.a
include $(PREBUILT_STATIC_LIBRARY)
