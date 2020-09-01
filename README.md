# Introduction

This is the eduVPN / Let's Connect! for Android application.

You can clone this repository by executing the following command (provided you have git installed):
    
    git clone --recurse-submodules https://github.com/eduvpn/android.git

Or if your git version is below 2.13:

    git clone --recursive https://github.com/eduvpn/android.git
    
# Organizations

A future version of the app will contain automatic organization discovery, where you can select your institute from a wide list.
This way you don't have to manually enter discovery URLs. To build this version of the app, build the `organization` flavor instead of `basic`.
 

# Running with Android Studio

First install the `swig` package with your operating system package manager.

Make sure you have the latest stable version of Android Studio installed, you can download it from [here](https://developer.android.com/studio).
Open the project by opening the build.gradle in the root of this repository with Android Studio.
Make sure that you have the following packages installed in the SDK Manager (Tools -> SDK Manager):
* SDK Platforms - Android 10
* SDK Tools - Android SDK Build Tools
* SDK Tools - LLDB
* SDK Tools - CMake
* SDK Tools - Android SDK Platform-Tools
* SDK Tools - Android SDK Tools
* SDK Tools - NDK - 19.2.x version (a later version might work, but is untested)

To find a specific version of a package (for the NDK), check the option 'Show Package Details'
in the bottom-lower corner of the SDK Manager.

The app should now build when selecting Run -> Run 'app', or clicking the green play button next
to the device selector.

# Building

**NOTE**: building Android applications takes a lot of resources. The VM used
to build the Let's Connect! application has 8GB of memory. Anything lower was
not tested, but 1GB definitely does not work.

## Dependencies

### Fedora

    $ sudo dnf -y install \
        unzip \
        tar \
        git \
        swig \
        java-1.8.0-openjdk \
        java-1.8.0-openjdk-devel \
        ncurses-compat-libs

We last tested this (successfully) on 2020-08-26 with Fedora 32.

### Debian

    $ sudo apt -y install openjdk-8-jdk git curl unzip swig ninja-build

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

### Git

To build the app from git, i.e. tag, branch or commit, use the following after 
modifying the file if necessary. Use this if you want to build a test or 
development release.

    $ ./build_app_git.sh

You'll find the signed output APK in ${HOME}/Projects.

### Tar

To build the app from tar, i.e. the archive that contains the full source code
including all submodules, use the following after modifying the file if 
necessary. Use this if you want to use an official release.

    $ ./build_app_tar.sh

You'll find the signed output APK in ${HOME}/Projects.
