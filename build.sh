#!/bin/sh

# TODO: determine exact versions of the SDK/NDK required
    export ANDROID_HOME="/opt/android-sdk" 
    export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:/opt/android-ndk:$PATH"
    export ANDROID_NDK=/opt/android-ndk
    export ANDROID_NDK_HOME=/opt/android-ndk
    #java home for openjdk
    export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")


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

./gradlew clean assembleRelease
#./gradlew clean assembleDebug

#apksigner sign --ks ~/my-release-key.jks app/build/outputs/apk/app-debug.apk

#sudo cp app/build/outputs/apk/app-debug.apk /var/www/html/
