#!/bin/sh
# script created for CentOS 7
# dependency: sudo

export ANDROID_HOME="/opt/android/sdk" 
export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:/opt/android/ndk:$PATH"
export ANDROID_NDK=/opt/android/ndk
export ANDROID_NDK_HOME=/opt/android/ndk
#java home for openjdk
export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")

sudo yum -y install java-1.8.0-openjdk-devel.x86_64 git wget unzip

#for Debian:
#sudo apt -y install openjdk-8-jdk git

# Prepare /opt
sudo mkdir -p /opt/android
sudo chown "$(id -un).$(id -gn)" /opt/android

# SDK
mkdir -p /opt/android/sdk
cd /opt/android/sdk || exit
wget https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip
unzip -q sdk-tools-linux-3859397.zip
ln -s sdk-tools-linux-3859397 sdk

yes | /opt/android/sdk/tools/bin/sdkmanager --licenses

# NDK
cd /opt/android || exit
wget https://dl.google.com/android/repository/android-ndk-r15c-linux-x86_64.zip
unzip -q android-ndk-r15c-linux-x86_64.zip
ln -s android-ndk-r15c ndk

# generate a keystore
#keytool -genkey -v -keystore ~/my-release-key.jks

rm -rf "${HOME}/eduvpn-app"
cd "${HOME}" || exit
git clone https://github.com/eduvpn/android.git eduvpn-app

cd eduvpn-app || exit
git submodule update --init --recursive

# build the library using the NDK
(
cd ics-openvpn/main || exit
./misc/build-native.sh
)

./gradlew clean assembleRelease
#./gradlew clean assembleDebug

#apksigner sign --ks ~/my-release-key.jks app/build/outputs/apk/app-debug.apk

#sudo cp app/build/outputs/apk/app-debug.apk /var/www/html/
