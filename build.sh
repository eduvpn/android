#!/bin/sh
# script created for CentOS 7
# dependency: sudo
# expected username: eduvpn

    export ANDROID_HOME="/opt/android-sdk" 
    export PATH="$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:/opt/android-ndk:$PATH"
    export ANDROID_NDK=/opt/android-ndk
    export ANDROID_NDK_HOME=/opt/android-ndk
    #java home for openjdk
    export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")

yes | sudo yum install java-1.8.0-openjdk-devel.x86_64 git
#for Debian:
#yes | sudo apt install openjdk-8-jdk git
#Download Android SDK tools command-line
mkdir ~/sdk-tools-linux-3859397; cd sdk-tools-linux-3859397
wget https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip
unzip sdk-tools-linux-3859397.zip
sudo mv ~/sdk-tools-linux-3859397 /opt
cd /opt; sudo ln -s sdk-tools-linux-3859397 android-sdk
sudo chown -R root.eduvpn sdk-tools-linux-3859397
sudo chmod g+w sdk-tools-linux-3859397/
#all licenses accept
yes | sudo /opt/android-sdk/tools/bin/sdkmanager --licenses
#Install Android NDK
cd ~;wget https://dl.google.com/android/repository/android-ndk-r15c-linux-x86_64.zip
unzip android-ndk-r15c-linux-x86_64.zip
sudo mv ./android-ndk-r15c /opt
cd /opt
sudo chown -R root.eduvpn android-ndk-r15c
sudo ln -s android-ndk-r15c android-ndk

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
