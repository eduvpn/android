#!/bin/sh

###############################################################################
# CONFIGURATION
###############################################################################

SDK_DIR=${HOME}/android-rebuilds-sdk
KEY_STORE=${HOME}/android.jks

V=2.0.4
DOWNLOAD_URL=https://github.com/eduvpn/android/releases/download/${V}/eduvpn-android-${V}.tar.xz

PROJECT_DIR=${HOME}/Projects
APP_DIR=${PROJECT_DIR}/eduvpn-android-${V}

# eduVPN
GRADLE_TASK=app:assembleBasicRelease
UNSIGNED_APK=${APP_DIR}/app/build/outputs/apk/basic/release/app-basic-release-unsigned.apk 
SIGNED_APK=${PROJECT_DIR}/eduVPN-${V}.apk

# Let's Connect!
#GRADLE_TASK=app:assembleHomeRelease
#UNSIGNED_APK=${APP_DIR}/app/build/outputs/apk/home/release/app-home-release-unsigned.apk 
#SIGNED_APK=${PROJECT_DIR}/LetsConnect-${V}.apk

###############################################################################
# CLONE
###############################################################################

(
    mkdir -p "${PROJECT_DIR}"
    cd "${PROJECT_DIR}" || exit

    curl -L -o ${PROJECT_DIR}/eduvpn-android-${V}.tar.xz "${DOWNLOAD_URL}"
    tar -xJf ${PROJECT_DIR}/eduvpn-android-${V}.tar.xz
)

###############################################################################
# PATCH
###############################################################################

(
    SCRIPT_DIR=${PWD}
    cd "${APP_DIR}" || exit
    echo "Patching extracted sources"
    # Checking beforehand if patch can be applied or not:
    # If reversing the patch (-R) works that means the patch was already applied, do a dry run to not apply anything (--dry-run) and do not output anything extra for it (-s)
    # This all means reversing the patch will fail succesfully™ (hence -s will not silence _all_ errors)
    # If the patch was already applied the second command will not execute (OR operator)
    # If reversing the patch does not work it will actually patch the to be changed files
    patch -p1 -s < ${SCRIPT_DIR}/patches/android-rebuilds/ar.patch
    echo "Patched extracted sources"
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
