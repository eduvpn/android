# Introduction

This is the eduVPN / Let's Connect! for Android application.

# Building

## Dependencies

    $ sudo yum -y install unzip git java-1.8.0-openjdk java-1.8.0-openjdk-devel

## Key Store

Generate a key store for signing the Android application:

    $ keytool -genkey -v -keystore ${HOME}/android.jks

## Setup

    $ ./builder_setup.sh

## Build

    $ ./build_app.sh
