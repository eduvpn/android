//
// Created by Dániel Zolnai on 2023. 09. 25..
//
#include <jni.h>
#include <string>
#include "../../../libs/eduvpn-common/exports/lib/android/eduvpn_common.h"

typedef int (*StateCB)(int oldstate, int newstate, void* data);

// Implement a function that matches the StateCB signature
int createStateCallback(int oldstate, int newstate, void* data) {
        // TODO update if needed
        printf("Callback called with oldstate: %d, newstate: %d\n", oldstate, newstate);
        return 0;  // Return an example value
}

extern char* Register(char *name, char *version, char *configDirectory, StateCB cb, int debug);


extern "C" JNIEXPORT jstring JNICALL
Java_org_eduvpn_common_GoBackend_register(JNIEnv *env, jobject /* this */, jstring name, jstring version, jstring configDirectory, jobject cb, jint debug)
{
        const char *name_str = env->GetStringUTFChars(name, nullptr);
        const char *version_str = env->GetStringUTFChars(version, nullptr);
        const char *configDirectory_str = env->GetStringUTFChars(configDirectory, nullptr);
        StateCB callbackFunction = createStateCallback;
        int debug_int = (int)debug;

        char *result = Register(
                (char *)name_str,
                (char *)version_str,
                (char *)configDirectory_str,
                callbackFunction,
                debug_int
                );
        env->ReleaseStringUTFChars(name, name_str);
        env->ReleaseStringUTFChars(version, version_str);
        env->ReleaseStringUTFChars(configDirectory, configDirectory_str);
        return env->NewStringUTF(result);
}
