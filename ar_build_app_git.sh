#!/bin/sh

###############################################################################
# CONFIGURATION
###############################################################################

SDK_DIR=${HOME}/android-rebuilds-sdk
KEY_STORE=${HOME}/android.jks

GIT_REPO=https://github.com/eduvpn/android
GIT_TAG=2.0.5
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

    git clone --recursive -b ${GIT_TAG} ${GIT_REPO} "${APP_DIR}"
)

###############################################################################
# PATCH
###############################################################################

(
    SCRIPT_DIR=${PWD}
    cd "${APP_DIR}" || exit
    echo "Patching cloned repo"
    # Checking beforehand if patch can be applied or not:
    # Patching current repo
    # Checking beforehand if patch can be applied or not:
    # If reversing the patch (-R) works that means the patch was already applied, do a dry run to not apply anything (--check)
    # This all means reversing the patch will fail succesfully™
    # If the patch was already applied the second command will not execute (OR operator)
    # If reversing the patch does not work it will actually patch the to be changed files
    git apply ${SCRIPT_DIR}/patches/android-rebuilds/ar.patch
    echo "Patched cloned repo"
 )

###############################################################################
# BUILD
###############################################################################

(
    export ANDROID_HOME=${SDK_DIR}
    cd "${APP_DIR}" || exit
    ./gradlew ${GRADLE_TASK} --warning-mode all --stacktrace || exit
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
