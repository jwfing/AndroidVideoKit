#include "ffmpegcall.h"
#include <dlfcn.h>

void *handle;
typedef int (*func)(int, char **); // define function prototype
func process; // some name for the function

int ffmpegcall(int argc, char **argv) {
	handle = dlopen("/data/data/com.avos.mixbit/lib/libffmpegutils.so", RTLD_LAZY);
	process = (func)dlsym(handle, "process");
	int ret = process(argc, argv);
	dlclose(handle);
	return ret;
}
