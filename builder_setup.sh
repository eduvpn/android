#!/bin/sh

###############################################################################
# CONFIGURATION
###############################################################################

SDK_DIR=${HOME}/android-sdk

# current as of 20240117
# see https://developer.android.com/studio/#downloads "Command line tools only"
SDK_VERSION=11076708

# Always use latest from https://developer.android.com/studio/releases/build-tools
BUILD_TOOLS_VERSION=34.0.0

# see app/build.gradle for "targetSdkVersion"
PLATFORM_VERSION=34

# see app/build.gradle for "ndkVersion"
NDK_VERSION=26.1.10909125

# should not require modification...
SDK_URL=https://dl.google.com/android/repository/commandlinetools-linux-${SDK_VERSION}_latest.zip

###############################################################################
# SETUP
###############################################################################

# create and populate SDK directory
(
    mkdir -p "${SDK_DIR}"
    cd "${SDK_DIR}" || exit
    curl -L -O ${SDK_URL}
    unzip -q commandlinetools-linux-${SDK_VERSION}_latest.zip
    rm commandlinetools-linux-${SDK_VERSION}_latest.zip
)

# accept licenses
(
    cd "${SDK_DIR}" || exit
    yes | cmdline-tools/bin/sdkmanager --sdk_root=${SDK_DIR} --licenses
)

# install required SDK components
(
    cd "${SDK_DIR}" || exit
    cmdline-tools/bin/sdkmanager --sdk_root=${SDK_DIR} --update
    cmdline-tools/bin/sdkmanager --sdk_root=${HOME}/android-sdk "ndk;${NDK_VERSION}"
    cmdline-tools/bin/sdkmanager --sdk_root=${HOME}/android-sdk "build-tools;${BUILD_TOOLS_VERSION}"
    cmdline-tools/bin/sdkmanager --sdk_root=${HOME}/android-sdk "platforms;android-${PLATFORM_VERSION}"
)
