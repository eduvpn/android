#!/bin/sh

###############################################################################
# CONFIGURATION
###############################################################################

SDK_DIR=${HOME}/android-sdk
KEY_STORE=${HOME}/android.jks

GIT_REPO=https://github.com/eduvpn/android
GIT_TAG=1.2.3
#GIT_TAG=master

PROJECT_DIR=${HOME}/Projects
APP_DIR=${PROJECT_DIR}/eduvpn-android-$(date +%Y%m%d%H%M%S)

# eduVPN
GRADLE_TASK=app:assembleBasicRelease
UNSIGNED_APK=${APP_DIR}/app/build/outputs/apk/basic/release/app-basic-release-unsigned.apk 
SIGNED_APK=${PROJECT_DIR}/eduVPN-${GIT_TAG}.apk

# Let's Connect!
#GRADLE_TASK=app:assembleHomeRelease
#UNSIGNED_APK=${APP_DIR}/app/build/outputs/apk/home/release/app-home-release-unsigned.apk 
#SIGNED_APK=${PROJECT_DIR}/LetsConnect-${GIT_TAG}.apk

###############################################################################
# CLONE
###############################################################################

(
    mkdir -p "${PROJECT_DIR}"
    cd "${PROJECT_DIR}" || exit

    git clone --recursive -b ${GIT_TAG} ${GIT_REPO} "${APP_DIR}"
    cd "${APP_DIR}" || exit
)

###############################################################################
# BUILD
###############################################################################

(
    export ANDROID_HOME=${SDK_DIR}
    cd "${APP_DIR}" || exit
    ./gradlew ${GRADLE_TASK} --stacktrace || exit
)

###############################################################################
# SIGN
###############################################################################

(
    # pick the newest build tools in case multiple versions are available
    BUILD_TOOLS_VERSION=$(ls ${SDK_DIR}/build-tools/ | sort -r | head -1)
    ${SDK_DIR}/build-tools/${BUILD_TOOLS_VERSION}/apksigner sign --ks "${KEY_STORE}" "${UNSIGNED_APK}" || exit
    cp "${UNSIGNED_APK}" "${SIGNED_APK}" || exit
)
