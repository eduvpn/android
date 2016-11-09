#!/bin/sh

# TODO: determine exact versions of the SDK/NDK required
export ANDROID_HOME=${HOME}/Downloads/android-sdk-linux
export ANDROID_NDK=${HOME}/Downloads/android-ndk-r12
export PATH=$PATH:${ANDROID_HOME}/build-tools/24.0.3:${ANDROID_NDK}

# generate a keystore
#keytool -genkey -v -keystore ~/my-release-key.jks

cd "${HOME}"
rm -rf "${HOME}/eduvpn-app"
git clone https://github.com/eduvpn/android.git eduvpn-app

cd eduvpn-app
git submodule update --init --recursive

# build the library using the NDK
(
cd ics-openvpn/main
./misc/build-native.sh
)

#./gradlew clean assembleRelease
./gradlew clean assembleDebug

apksigner sign --ks ~/my-release-key.jks app/build/outputs/apk/app-debug.apk

#sudo cp app/build/outputs/apk/app-debug.apk /var/www/html/
