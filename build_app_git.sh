#!/bin/sh

###############################################################################
# CONFIGURATION
###############################################################################

SDK_DIR=${HOME}/android-sdk
KEY_STORE=${HOME}/android.jks

GIT_REPO=https://codeberg.org/eduVPN/android
#GIT_TAG=3.3.3
GIT_TAG=master

PROJECT_DIR=${HOME}/Projects
APP_DIR=${PROJECT_DIR}/eduvpn-android-$(date +%Y%m%d%H%M%S)

# eduVPN
KEY_ALIAS=eduVPN
GRADLE_TASKS="app:assembleBasicRelease app:bundleBasicRelease"
UNSIGNED_APK=${APP_DIR}/app/build/outputs/apk/basic/release/app-basic-release-unsigned.apk
UNSIGNED_AAB=${APP_DIR}/app/build/outputs/bundle/basicRelease/app-basic-release.aab
SIGNED_APK=${PROJECT_DIR}/eduVPN-${GIT_TAG}.apk
SIGNED_AAB=${PROJECT_DIR}/eduVPN-${GIT_TAG}.aab

# govVPN
#KEY_ALIAS=govVPN
#GRADLE_TASKS="app:assembleGovRelease app:bundleGovRelease"
#UNSIGNED_APK=${APP_DIR}/app/build/outputs/apk/gov/release/app-gov-release-unsigned.apk
#UNSIGNED_AAB=${APP_DIR}/app/build/outputs/bundle/govRelease/app-gov-release.aab
#SIGNED_APK=${PROJECT_DIR}/govVPN-${GIT_TAG}.apk
#SIGNED_AAB=${PROJECT_DIR}/govVPN-${GIT_TAG}.aab

# Let's Connect!
#KEY_ALIAS=LC
#GRADLE_TASKS="app:assembleHomeRelease app:bundleHomeRelease"
#UNSIGNED_APK=${APP_DIR}/app/build/outputs/apk/home/release/app-home-release-unsigned.apk
#UNSIGNED_AAB=${APP_DIR}/app/build/outputs/bundle/homeRelease/app-home-release.aab
#SIGNED_APK=${PROJECT_DIR}/LetsConnect-${GIT_TAG}.apk
#SIGNED_AAB=${PROJECT_DIR}/LetsConnect-${GIT_TAG}.aab

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
    for GRADLE_TASK in ${GRADLE_TASKS}; do
        ./gradlew "${GRADLE_TASK}" --warning-mode all --stacktrace || exit
    done
)

###############################################################################
# SIGN
###############################################################################

(
    # pick the newest build tools in case multiple versions are available
    BUILD_TOOLS_VERSION=$(ls ${SDK_DIR}/build-tools/ | sort -r | head -1)

    
    # sign the APK
    ${SDK_DIR}/build-tools/${BUILD_TOOLS_VERSION}/apksigner sign --ks "${KEY_STORE}" "${UNSIGNED_APK}" || exit
    cp "${UNSIGNED_APK}" "${SIGNED_APK}" || exit
    
    # sign the AAB
    jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore "${KEY_STORE}" "${UNSIGNED_AAB}" "${KEY_ALIAS}" || exit
    cp "${UNSIGNED_AAB}" "${SIGNED_AAB}" || exit
)
