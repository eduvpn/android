#!/bin/sh

###############################################################################
# CONFIGURATION
###############################################################################

SDK_DIR=${HOME}/android-sdk
KEY_STORE=${HOME}/android.jks

GIT_REPO=https://github.com/eduvpn/android
GIT_TAG=1.2.1

PROJECT_DIR=${HOME}/Projects
APP_DIR=${PROJECT_DIR}/eduvpn-android-$(date +%Y%m%d%H%M%S)

# eduVPN
GRADLE_TASK=app:assembleBasicRelease
UNSIGNED_APK=${APP_DIR}/app/build/outputs/apk/basic/release/app-basic-release-unsigned.apk 
SIGNED_APK=${PROJECT_DIR}/eduVPN-${GIT_TAG}.apk

# Let's Connect!
#GRADLE_TASK=app:assembleHomeRelease
#UNSIGNED_APK=${APP_DIR}/app/build/outputs/apk/basic/release/app-home-release-unsigned.apk 
#SIGNED_APK=${PROJECT_DIR}/LetsConnect-${GIT_TAG}.apk

###############################################################################
# CLONE
###############################################################################

(
    mkdir -p ${PROJECT_DIR}
    cd ${PROJECT_DIR} || exit

    git clone -b ${GIT_TAG} ${GIT_REPO} ${APP_DIR}
    cd ${APP_DIR}
    git submodule update --init --recursive
)

###############################################################################
# BUILD
###############################################################################

(
    export ANDROID_HOME=${SDK_DIR}
    cd ${APP_DIR}
    ./gradlew ${GRADLE_TASK}
)

###############################################################################
# SIGN
###############################################################################

(
    ${SDK_DIR}/build-tools/*/apksigner sign --ks ${KEY_STORE} ${OUTPUT_APK}
    cp ${UNSIGNED_APK} ${SIGNED_APK}
)
