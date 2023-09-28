//
// Created by Dániel Zolnai on 2023. 09. 25..
//
#include <jni.h>
#include <string>
#include <android/log.h>
#include "../../../libs/eduvpn-common/exports/lib/android/eduvpn_common.h"

static JavaVM *globalVM;
static jclass globalBackendClass;
static jclass globalCallbackClass;


bool GetJniEnv(JavaVM *vm, JNIEnv **env) {
    bool did_attach_thread = false;
    *env = nullptr;
    // Check if the current thread is attached to the VM
    auto get_env_result = vm->GetEnv((void**)env, JNI_VERSION_1_6);
    if (get_env_result == JNI_EDETACHED) {
        if (vm->AttachCurrentThread(env, nullptr) == JNI_OK) {
            did_attach_thread = true;
        } else {
            // Failed to attach thread. Throw an exception if you want to.
        }
    } else if (get_env_result == JNI_EVERSION) {
        // Unsupported JNI version. Throw an exception if you want to.
    }
    return did_attach_thread;
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

int callGlobalCallback(int newstate, void *data) {
    if (!globalVM) {
        return 0;
    }
    JNIEnv *env;
    bool didAttach = GetJniEnv(globalVM, &env);
    jfieldID callbackFieldId = env->GetStaticFieldID(globalBackendClass, "callbackFunction",
                                                     "Lorg/eduvpn/common/GoBackend$Callback;");
    jobject callbackField = env->GetStaticObjectField(globalBackendClass, callbackFieldId);
    jmethodID callbackFunction = env->GetMethodID(globalCallbackClass, "onNewState", "(ILjava/lang/String;)Z");

    jboolean didHandle;

    if (!data) {
        didHandle = env->CallBooleanMethod(callbackField, callbackFunction, newstate, nullptr);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "NATIVECOMMON", "ERRORCallback: %d, data: %s\n",
                            newstate, (char *) data);
        jstring dataString = env->NewStringUTF((char *) data);
        // We do not call FreeString(...) here on data, because it is already done by the Common library.
        didHandle = env->CallBooleanMethod(callbackField, callbackFunction, newstate, dataString);
    }
    if (didAttach) {
        globalVM->DetachCurrentThread();
    }
    return didHandle ? 1 : 0;
}

// Implement a function that matches the StateCB signature
int createStateCallback(int oldstate, int newstate, void *data) {
    __android_log_print(ANDROID_LOG_ERROR, "NATIVECOMMON", "Callback called with oldstate: %d, newstate: %d, data: %s\n", oldstate, newstate, (char *)data);
    return callGlobalCallback(newstate, data);
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_eduvpn_common_GoBackend_register(JNIEnv *env, jobject /* this */, jstring name, jstring version, jstring configDirectory, jboolean debug) {
    env->GetJavaVM(&globalVM);
    const char *name_str = env->GetStringUTFChars(name, nullptr);
    const char *version_str = env->GetStringUTFChars(version, nullptr);

    const char *configDirectory_str = nullptr;
    if (configDirectory != nullptr) {
            configDirectory_str = env->GetStringUTFChars(configDirectory, nullptr);
    }
    // Set up callbacks

    jclass backendCls = env->FindClass("org/eduvpn/common/GoBackend");

    jclass callbackClass = env->FindClass("org/eduvpn/common/GoBackend$Callback");

    globalCallbackClass = (jclass)env->NewGlobalRef(callbackClass);
    globalBackendClass = (jclass)env->NewGlobalRef(backendCls);

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
    // Do not delete the cookie, because it might be reused later in the flow
    env->ReleaseStringUTFChars(id, id_str);
    if (error != nullptr) {
        jstring nativeError = env->NewStringUTF(error);
        FreeString(error);
        return nativeError;
    }
    return nullptr;
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_eduvpn_common_GoBackend_handleRedirection(JNIEnv *env, jobject /* this */, jint cookie, jstring url) {
    const char *url_str = env->GetStringUTFChars(url, nullptr);
    char *error = CookieReply((uintptr_t)cookie, (char *)url_str);
    env->ReleaseStringUTFChars(url, url_str);
    __android_log_print(ANDROID_LOG_ERROR, "NATIVECOMMON", "Cookie reply: %d %s\n", cookie, url_str);
    return NativeStringToJString(env, error);
}

extern "C" JNIEXPORT jobject JNICALL
Java_org_eduvpn_common_GoBackend_getAddedServers(JNIEnv *env, jobject /* this */) {
    ServerList_return result = ServerList();
    return CreateDataErrorTuple(env, result.r0, result.r1);
}
extern "C" JNIEXPORT jstring JNICALL
Java_org_eduvpn_common_GoBackend_removeServer(JNIEnv *env, jobject /* this */, jint serverType, jstring id) {
    const char *id_str = env->GetStringUTFChars(id, nullptr);
    char *error = RemoveServer((int)serverType, (char *)id_str);
    env->ReleaseStringUTFChars(id, id_str);
    return NativeStringToJString(env, error);

}
extern "C" JNIEXPORT jobject JNICALL
Java_org_eduvpn_common_GoBackend_getProfiles(JNIEnv *env, jobject /* this */, jint serverType, jstring id, jboolean preferTcp, jboolean isStartUp) {
    const char *id_str = env->GetStringUTFChars(id, nullptr);
    uintptr_t cookie = CookieNew();
    GetConfig_return result = GetConfig(cookie, (int)serverType, (char *)id_str, (int)preferTcp, (int)isStartUp);
    env->ReleaseStringUTFChars(id, id_str);
    CookieDelete(cookie);
    return CreateDataErrorTuple(env, result.r0, result.r1);
}
extern "C" JNIEXPORT jstring JNICALL
Java_org_eduvpn_common_GoBackend_selectProfile(JNIEnv *env, jobject /* this */, jint cookie, jstring profileId) {
    const char *profileId_str = env->GetStringUTFChars(profileId, nullptr);
    char *error = CookieReply((uintptr_t)cookie, (char *)profileId_str);
    env->ReleaseStringUTFChars(profileId, profileId_str);
    return NativeStringToJString(env, error);
}