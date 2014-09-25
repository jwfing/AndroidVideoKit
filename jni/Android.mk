LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := wanpainative
LOCAL_SRC_FILES := com_avos_minute_util_VideoEngine.c ffmpeg.c ffmpeg_opt.c ffmpeg_filter.c cmdutils.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_LDLIBS := -L$(NDK_PLATFORMS_ROOT)/$(TARGET_PLATFORM)/arch-arm/usr/lib -L$(LOCAL_PATH) -lavfilter -lavformat -lavcodec -lavdevice -lavutil -lswresample -lswscale -lpostproc -llog -ljnigraphics -lx264 -lz -ldl -lgcc
include $(BUILD_SHARED_LIBRARY)
