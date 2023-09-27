//
// Created by Dániel Zolnai on 2023. 09. 25..
//
#include <jni.h>
#include <string>
#include <android/log.h>
#include "../../../libs/eduvpn-common/exports/lib/android/eduvpn_common.h"

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

JNIEnv *globalEnv;


void callGlobalCallback(int newstate, void *data) {

    jclass backendCls = globalEnv->FindClass("org/eduvpn/common/GoBackend");
    jfieldID callbackFieldId = globalEnv->GetStaticFieldID(backendCls, "callbackFunction", "Lorg/eduvpn/common/GoBackend$Callback;");

    jobject callbackField = globalEnv->GetStaticObjectField(backendCls, callbackFieldId);
    jclass callbackClass = globalEnv->FindClass("org/eduvpn/common/GoBackend$Callback");
    jmethodID callbackFunction = globalEnv->GetMethodID(callbackClass, "onNewState", "(ILjava/lang/String;)V");
    if (!data) {
        globalEnv->CallVoidMethod(callbackField, callbackFunction, newstate, nullptr);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "NATIVECOMMON", "ERRORCallback: %d, data: %s\n", newstate, (char *)data);
        jstring dataString = globalEnv->NewStringUTF((char *)data);
        // We do not call FreeString(...) here on data, because it is already done by the Common library.
        globalEnv->CallVoidMethod(callbackField, callbackFunction, newstate, dataString);
    }
}

// Implement a function that matches the StateCB signature
int createStateCallback(int oldstate, int newstate, void *data) {
    __android_log_print(ANDROID_LOG_ERROR, "NATIVECOMMON", "Callback called with oldstate: %d, newstate: %d, data: %s\n", oldstate, newstate, (char *)data);
    callGlobalCallback(newstate, data);
    return 0;  // Return an example value
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_eduvpn_common_GoBackend_register(JNIEnv *env, jobject /* this */, jstring name, jstring version, jstring configDirectory, jboolean debug) {
    globalEnv = env;
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
    CookieDelete(cookie);
    return CreateDataErrorTuple(env, serversReturn.r0, serversReturn.r1);

}

extern "C" JNIEXPORT jstring JNICALL
Java_org_eduvpn_common_GoBackend_addServer(JNIEnv *env, jobject /* this */, jint serverType, jstring id) {
    uintptr_t cookie = CookieNew();
    const char *id_str = env->GetStringUTFChars(id, nullptr);
    char *error = AddServer(cookie, (int)serverType, (char *)id_str, 0);
    CookieDelete(cookie);
    env->ReleaseStringUTFChars(id, id_str);
    if (error != nullptr) {
        jstring nativeError = env->NewStringUTF(error);
        FreeString(error);
        return nativeError;
    }
    return nullptr;
}