LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := ffmpegutils
LOCAL_SRC_FILES := com_avos_minute_util_VideoEngine.c ffmpeg.c cmdutils.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_LDLIBS := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib -L$(LOCAL_PATH) -lavformat -lavcodec -lavdevice -lavfilter -lavutil -lavresample -lswresample -lswscale -lpostproc -llog -ljnigraphics -lz -ldl -lgcc
 
include $(BUILD_SHARED_LIBRARY)
