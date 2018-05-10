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

    $ keytool -genkey -v -keystore ${HOME}/android.jks

## Setup

    $ ./builder_setup.sh

## Build

    $ ./build_app.sh

You'll find the signed output APK in ${HOME}/Projects.
