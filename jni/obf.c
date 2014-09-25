#include "Base64EncodeDecode.h"

#ifndef DEBUG_ANDROID
#include <android/log.h>

#define LOG_TAG "NDK_Mixbit"
//#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
//#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)
#define  LOGE(...)

#else
#define  LOGI(...)
#define  LOGE(...)
#endif

#define STATIC_SIZEOF_SZ(x) sizeof(x)/sizeof(x[0])
#define BOOL unsigned int
#define SWP(x,y) (x^=y, y^=x, x^=y)

// http://stackoverflow.com/questions/198199/how-do-you-reverse-a-string-in-place-in-c-or-c
void strrev(char *p) {
	char *q = p;
	while (q && *q)
		++q; /* find eos */
	for (--q; p < q; ++p, --q)
		SWP(*p, *q);
}

void strrev_utf8(char *p) {
	char *q = p;
	strrev(p); /* call base case */

	/* Ok, now fix bass-ackwards UTF chars. */
	while (q && *q)
		++q; /* find eos */
	while (p < --q)
		switch ((*q & 0xF0) >> 4) {
		case 0xF: /* U+010000-U+10FFFF: four bytes. */
			SWP(*(q-0), *(q-3));
			SWP(*(q-1), *(q-2));
			q -= 3;
			break;
		case 0xE: /* U+000800-U+00FFFF: three bytes. */
			SWP(*(q-0), *(q-2));
			q -= 2;
			break;
		case 0xC: /* fall-through */
		case 0xD: /* U+000080-U+0007FF: two bytes. */
			SWP(*(q-0), *(q-1));
			q--;
			break;
		}
}

// http://www.cprogramming.com/snippets/source-code/a-function-to-encryptdividedecrypt-a-string-using-xor-e
// let's hope the buffer is long enough!
unsigned int XOR(const char *value, int valuelen, const char *key, char *retval, int bufflen) {
	unsigned int klen = strlen(key);
	unsigned int vlen = valuelen;
	unsigned int k = 0;
	unsigned int v = 0;

	for (v; v < vlen && v < bufflen - 1; v++) {
		retval[v] = value[v] ^ key[k];
		//k = (++k < klen ? k : 0);

		// roll k around
		k++;
		k = k % klen;
	}

	// null terminate
	retval[v] = '\0';

	return v;
}

void stripe(char *key, int stripe, char *retval, int bufflen) {
	// start at the first stripe
	unsigned int s = stripe;
	unsigned int i = 0;

	while (*key && s < bufflen - 1) {
		if (s % stripe == 0) {
			retval[i] = key[s];
			i++;
		}
		s++;
	}

	// null terminate
	retval[i] = '\0';
}

char *concat(const char *strings[], int len) {
	int totallen = 0;
	int i = 0;

	for (i = 0; i < len; i++) {
		totallen += strlen(strings[i]);
	}

	char *concatStr = (char *) malloc(totallen + 1);
	char *concatPtr = concatStr;
	for (i = 0; i < len; i++) {
		const char *currStr = strings[i];

		// copy the strings
		while (*concatPtr++ = *currStr++)
			;

		// back up over the null terminator
		concatPtr--;
	}

	// the string is already null-terminated
	return concatStr;
}

// http://stackoverflow.com/questions/4161822/remove-all-occurences-of-a-character-in-c-string-example-needed
void stripchars(char *str, char stripch) {
	char *s;
	char *d;

	for (s = d = str; *d = *s; d += (*s++ != stripch))
		;
}

int obf(const char *src, char *key, char *dest, int destlen) {
	unsigned char buff[512];

	memset(buff, 0, 512);
	LOGE("source text =                     \"%s\", length = %d", src, strlen(src));

	unsigned int len = XOR(src, strlen(src), key, buff, 512);
	//LOGE("cipher text = \"%s\", length = %d, actual buffer used length = %d", buff, strlen(buff), len);

	base64_encode(buff, len, dest, destlen);
	LOGE("base64(cipher text) =             \"%s\", length = %d", dest, strlen(dest));

	// reverse
	int thirdLastPosn = strlen(dest) - 3;
	char thirdLastChar = dest[thirdLastPosn];
	dest[thirdLastPosn] = '\0';
	strrev(dest);
	dest[thirdLastPosn] = thirdLastChar;
	LOGE("rev3(base64(cipher text)) =       \"%s\", length = %d", dest, strlen(dest));

	return strlen(dest);
}

int unobf(const char *src, char *key, char *dest, int destlen) {
	unsigned char buff[512];
	unsigned char buff2[512];

	memset(buff, 0, 512);
	memset(buff2, 0, 512);
	strncpy(buff2, src, 512);
	// unreverse

	LOGE("encrypted text =                  \"%s\", length = %d", buff2, strlen(buff2));

	int thirdLastPosn = strlen(buff2) - 3;
	char thirdLastChar = buff2[thirdLastPosn];
	buff2[thirdLastPosn] = '\0';
	strrev(buff2);
	buff2[thirdLastPosn] = thirdLastChar;
	LOGE("rev3(rev3(base64(cipher text))) = \"%s\", length = %d", buff2, strlen(buff2));

	int len = base64_decode(buff2, buff, 512);
	//LOGE("unbase64(base64(cipher text)) = \"%s\", length = %d, actual buffer used length = %d", buff, strlen(buff), len);

	len = XOR(buff, len, key, dest, destlen);
	LOGE("decrypted text =                  \"%s\", actual buffer used length = %d", dest, strlen(dest), len);

	return strlen(dest);
}
