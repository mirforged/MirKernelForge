#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_xin_micro_kp_moduleloader_utils_NativeApi_syscall(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}