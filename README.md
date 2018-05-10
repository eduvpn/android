# Introduction

This is the eduVPN / Let's Connect! for Android application.

# Building

We use a Fedora 28 build machine.

## Dependencies

    $ sudo dnf -y install \
        unzip \
        git \
        java-1.8.0-openjdk \
        java-1.8.0-openjdk-devel \
        ncurses-compat-libs

## Key Store

Generate a key store for signing the Android application:

    $ keytool -genkey -v -keystore ${HOME}/android.jks

## Setup

    $ ./builder_setup.sh

## Build

    $ ./build_app.sh
