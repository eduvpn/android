//
// Created by Dániel Zolnai on 2023. 09. 25..
//
#include <jni.h>
#include <string>

//struct go_string { const char *str; long n; };
typedef int (*StateCB)(int oldstate, int newstate, void* data);

extern char* Register(char *name, char *version, char *configDirectory, StateCB cb, int debug);

extern "C" JNIEXPORT jstring JNICALL
Java_org_eduvpn_common_GoBackend_register(JNIEnv *env, jobject /* this */, jstring name, jstring version, jstring configDirectory, jobject cb, jint debug)
{
        std::string hello = "Hello from C++";
        return env->NewStringUTF(hello.c_str());
}
