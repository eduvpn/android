//
// Created by Dániel Zolnai on 2023. 09. 25..
//
#include <jni.h>
#include <string>
#include <android/log.h>
#include "../../../libs/eduvpn-common/exports/lib/android/eduvpn_common.h"

typedef int (*StateCB)(int oldstate, int newstate, void *data);

// Implement a function that matches the StateCB signature
int createStateCallback(int oldstate, int newstate, void *data) {
        // TODO update if needed
        printf("Callback called with oldstate: %d, newstate: %d\n", oldstate, newstate);
        return 0;  // Return an example value
}

void throwJavaException(JNIEnv *env, const char *msg)
{
    // You can put your own exception here
    jclass c = env->FindClass("java/lang/NullPointerException");
    env->ThrowNew(c, msg);
}

jstring NativeStringToJString(JNIEnv *env, char *nativeString) {
    if (nativeString == nullptr) {
        return nullptr;
    }
    jstring result = env->NewStringUTF(nativeString);
    FreeString(nativeString);
    return result;
}

jobject CreateDataErrorTuple(JNIEnv *env, char *data, char *error) {
    jstring dataString = NativeStringToJString(env, data);
    jstring errorString = NativeStringToJString(env, error);
    jclass dataErrorCls = env->FindClass("org/eduvpn/common/DataErrorTuple");
    jmethodID constructor = env->GetMethodID(dataErrorCls, "<init>","(Ljava/lang/String;Ljava/lang/String;)V");
    jobject object = env->NewObject( dataErrorCls, constructor, dataString, errorString);
    return object;
}


extern "C" JNIEXPORT jstring JNICALL
Java_org_eduvpn_common_GoBackend_register(JNIEnv *env, jobject /* this */, jstring name, jstring version, jstring configDirectory, jboolean debug) {
    const char *name_str = env->GetStringUTFChars(name, nullptr);
    const char *version_str = env->GetStringUTFChars(version, nullptr);

    const char *configDirectory_str = nullptr;
    if (configDirectory != nullptr) {
            configDirectory_str = env->GetStringUTFChars(configDirectory, nullptr);
    }
    StateCB callbackFunction = createStateCallback;
    int debug_int = (int) debug;
    char *nativeResult;
    try {
        nativeResult = Register(
                (char *) name_str,
                (char *) version_str,
                (char *) configDirectory_str,
                callbackFunction,
                debug_int
        );
    } catch (const std::exception& ex)
    {
        throwJavaException(env, ex.what());
    }
    env->ReleaseStringUTFChars(name, name_str);
    env->ReleaseStringUTFChars(version, version_str);
    if (configDirectory != nullptr) {
            env->ReleaseStringUTFChars(configDirectory, configDirectory_str);
    }
    return NativeStringToJString(env, nativeResult);
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_eduvpn_common_GoBackend_discoverOrganizations(JNIEnv *env, jobject /* this */) {
    uintptr_t cookie = CookieNew();
    DiscoOrganizations_return organizationsReturn = DiscoOrganizations(cookie);
    return CreateDataErrorTuple(env, organizationsReturn.r0, organizationsReturn.r1);
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_eduvpn_common_GoBackend_discoverServers(JNIEnv *env, jobject /* this */) {
    uintptr_t cookie = CookieNew();
    DiscoServers_return serversReturn = DiscoServers(cookie);
    return CreateDataErrorTuple(env, serversReturn.r0, serversReturn.r1);

}