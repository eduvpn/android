#!/bin/sh

###############################################################################
# CONFIGURATION
###############################################################################

SDK_DIR=${HOME}/android-sdk

# Android-Rebuilds F-Droid Mirror, will have to be changed to something hosted by SURF or the original AR repo considering the NDK is already unavailable
SDK_URL=https://mirror.f-droid.org/android-free/repository/

# Android-Rebuilds NDK mirror, temporary host because F-Droid does not host NDK version R21
NDK_URL=https://aaio.eu/ndk/
NDK_FILE=android-ndk-0-linux-x86_64.tar.bz2

declare -a arr=(
    "sdk-repo-linux-tools-26.1.1.zip" # Includes sdkmanager
    "sdk-repo-linux-platforms-eng.11.0.0_r27.zip" # To compile ics-openvpn
    "sdk-repo-linux-platforms-eng.10.0.0_r36.zip" # To compile eduvpn
    "sdk-repo-linux-platform-tools-eng.10.0.0_r14.zip" # Contains tools like adb and fastboot
    "sdk-repo-linux-build-tools-eng.10.0.0_r14.zip" # Contains tools like apksigner
)

###############################################################################
# SETUP
###############################################################################

# create and populate SDK directory
(
    mkdir -p "${SDK_DIR}" "${SDK_DIR}"/platforms "${SDK_DIR}"/build-tools "${SDK_DIR}"/ndk "${SDK_DIR}"/ndk/21.4.0
    cd "${SDK_DIR}" || exit
    for i in "${arr[@]}"
    do
        echo "Downloading $i"
        curl -L -O ${SDK_URL}/$i

        echo "Unzipping $i"
        unzip -q $i -d ${SDK_DIR}
        rm $i

        # Some of these zips need to be either placed in subfolders or have to be
        # renamed due to gradle warnings
        if [ $i = sdk-repo-linux-tools-26.1.1.zip ]
        then
            mv tools/ cmdline-tools/
        elif [ $i = sdk-repo-linux-build-tools-eng.10.0.0_r14.zip ]
        then
            mv android-10/ build-tools/29.0.2
        elif [ $i = sdk-repo-linux-platforms-eng.10.0.0_r36.zip ]
        then
            mv android-10/ platforms/android-29
        elif [ $i = sdk-repo-linux-platforms-eng.11.0.0_r27.zip ]
        then
            mv android-11/ platforms/android-30
        fi

        echo "Content of $i in place"
        echo ""
    done

    echo "Downloading ${NDK_FILE}. This might take a while depending on your connection"
    curl -L -O ${NDK_URL}/${NDK_FILE}
    echo "Extracting ${NDK_FILE}. This might take a while depending on your system."
    tar xjf ${NDK_FILE}
    rm ${NDK_FILE}
    mv android-ndk-r21e/* "${SDK_DIR}"/ndk/21.4.0/
    rmdir android-ndk-r21e/
    echo "Content of ${NDK_FILE} in place"
    echo ""
)