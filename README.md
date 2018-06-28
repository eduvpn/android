# Introduction

This is the eduVPN / Let's Connect! for Android application.

# Building

## Dependencies

### Fedora

    $ sudo dnf -y install \
        unzip \
        git \
        java-1.8.0-openjdk \
        java-1.8.0-openjdk-devel \
        ncurses-compat-libs

### Debian

    $ sudo apt -y install openjdk-8-jdk git curl unzip

## Key Store

Generate a key store for signing the Android application:

keytool -genkey -v -keystore LC.jks \
    -keyalg RSA -keysize 4096 -sigalg SHA256withRSA \
    -validity 10000 -dname "CN=Let's Connect!" -alias LC


    $ keytool \
        -genkey \
        -keystore ${HOME}/android.jks \
        -keyalg RSA \
        -keysize 4096 \
        -sigalg SHA256withRSA \
        -dname "CN=eduVPN for Android" \
        -validity 10000 \
        -alias eduVPN

Additional documentation 
[here](https://developer.android.com/studio/publish/app-signing#signing-manually).

## Setup

    $ ./builder_setup.sh

## Build

    $ ./build_app.sh

You'll find the signed output APK in ${HOME}/Projects.
