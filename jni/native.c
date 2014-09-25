#include <jni.h>
#include <string.h>
#include <android/log.h>

#include "obf.h"

#define LOG_TAG "NDK_Mixbit"
//#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
//#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)
#define  LOGE(...)

#define STATIC_SIZEOF_SZ(x) sizeof(x)/sizeof(x[0])

#define BOOL unsigned int
#define SWP(x,y) (x^=y, y^=x, x^=y)

static const char *STRINGS[] = { "14 Secret Services For The Wealthy", "Airbus A400M Set For July Deliveries", "House Prices Are Now Back To Normal",
		"What Do People Eat In Solitary Confinement", "14 Pictures Of Our Crowded World",
		"Shocking list of US foods that are BANNED in other countries for co..." };

jstring Java_com_avos_mixbit_Statics_convertString(JNIEnv *env, jobject thiz, jstring str, jboolean dir) {
	//__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "NDK:LC: [%s]", szLogThis);
	unsigned char keyBuff[512];
	unsigned char buff[512];
	unsigned char buff2[512];
	char *strings = concat(STRINGS, STATIC_SIZEOF_SZ(STRINGS));

	memset(keyBuff, 0, 512);
	memset(buff, 0, 512);
	memset(buff2, 0, 512);

	stripchars(strings, ' ');
	stripe(strings, 29, keyBuff, 512);
	// no idea why...but get it working for now
	keyBuff[8] = '\0';

	// ignore whether isCopy returns JNI_TRUE OR JNI_FALSE, always release
	// http://stackoverflow.com/questions/5859673/should-you-call-releasestringutfchars-if-getstringutfchars-returned-a-copy
	jboolean isCopy;
	const char *src = (*env)->GetStringUTFChars(env, str, &isCopy);


	LOGE(" ");
	LOGE(" ");
	LOGE("==============================================================");
	LOGE("Java_com_avos_mixbit_Statics_convertString\n\n");
	LOGE("==============================================================");

	LOGE("key = \"%s\", length = %d", keyBuff, strlen(keyBuff));
	//LOGE("source text = \"%s\", length = %d", src, strlen(src));

	if (dir) {
		unobf(src, keyBuff, buff2, 512);
	} else {
		obf(src, keyBuff, buff, 512);
		LOGE("encrypted text =                  \"%s\", length = %d", buff, strlen(buff));

		unobf(buff, keyBuff, buff2, 512);
		//LOGE("decrypted text = \"%s\", length = %d", buff2, strlen(buff2));
	}

	jstring js = (*env)->NewStringUTF(env, buff2);

	free(strings);
	(*env)->ReleaseStringUTFChars(env, str, src);

	return js;
}
