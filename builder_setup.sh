#!/bin/sh

###############################################################################
# CONFIGURATION
###############################################################################

SDK_DIR=${HOME}/android-sdk

# current as of 20180628
# see https://developer.android.com/studio/#downloads "Command line tools only"
SDK_VERSION=4333796

# see app/build.gradle for "buildToolsVersion"
BUILD_TOOLS_VERSION=27.0.3

# see app/build.gradle for "targetSdkVersion"
PLATFORM_VERSION=27

# should not require modification...
SDK_URL=https://dl.google.com/android/repository/sdk-tools-linux-${SDK_VERSION}.zip

###############################################################################
# SETUP
###############################################################################

# create and populate SDK directory
(
    mkdir -p "${SDK_DIR}"
    cd "${SDK_DIR}" || exit
    curl -L -O ${SDK_URL}
    unzip -q sdk-tools-linux-${SDK_VERSION}.zip
    rm sdk-tools-linux-${SDK_VERSION}.zip
)

# accept licenses
(
    cd "${SDK_DIR}" || exit
    yes | tools/bin/sdkmanager --licenses
)

# install required SDK components
(
    cd "${SDK_DIR}" || exit
    tools/bin/sdkmanager --update
    tools/bin/sdkmanager ndk-bundle
    tools/bin/sdkmanager "build-tools;${BUILD_TOOLS_VERSION}"
    tools/bin/sdkmanager "platforms;android-${PLATFORM_VERSION}"
)
