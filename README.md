# Introduction

This is the eduVPN / Let's Connect! for Android application.

# Building

## Dependencies

### Fedora

    $ sudo dnf -y install \
        unzip \
        git \
        swig \
        java-1.8.0-openjdk \
        java-1.8.0-openjdk-devel \
        ncurses-compat-libs

We last tested this (succesfully) on 2019-10-31 with Fedora 31.

### Debian

    $ sudo apt -y install openjdk-8-jdk git curl unzip

## Key Store

Generate a key store for signing the Android application:

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
