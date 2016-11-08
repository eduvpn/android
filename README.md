eduVPN
======

### Specification

The specification for the final version of this app can be found [here](https://github.com/eduvpn/documentation/tree/master/app).
This PoC is merely a skeleton which shows that downloading a profile and connecting to an OpenVPN server is possible.

### Building the project

Requirements:

* JDK 1.8 or higher
* Android SDK Platform 24 (7.0 Nougat)
* Android Studio 2.2.2 (if higher, the Gradle versions and the Android Gradle plugin might need version updates in the build files)

Instructions:

1. Clone this repository
2. Make sure you check out all the submodules (recursively!)

##### Linux

Run the **build.sh** file located in the root of this repository.

### Windows

1. For the native libraries, go to the [apk repository](http://plai.de/android/), and download the apk with same version as the submodule.
2. Rename the downloaded apk to a zip file, and open it with your archive manager. Unzip the `assets/` and `/lib` directories.
3. If the `assets/` directory contains an .html file, remove it.
4. Move the two extracted directories inside the project folder `android/app/src/main/ovpnlibs`. Rename the `lib` directory to `jniLibs`.
5. Now you can build the project with `gradlew assembleDebug` (or release)

### Tests

This app contains instrumented unit tests and also automated UI tests.

##### Instrumented unit tests

The tests can be found at `app\src\androidTest\java\<packagename>\UnitTestSuite.java`.
To run the tests from Android Studio, right click on the class name and select "Run 'UnitTestSuite'".
Since the tests are instrumented, they require an attached device or simulator to run on.
To run the same tests from the console, use the command `gradlew cAT`.
When the command has finished, you can view the report at `app\build\reports\androidTests\connected\index.html`.

##### Automated UI tests

These tests also run on the device, but they actually run the app and check for elements of the UI.
They have some prerequisites though.

1. You need to install the Appium server using `npm`: `npm install -g appium`
2. You also need to install the Ruby dependencies: `bundle install` (if you don't have the bundle command, first install it: `gem install bundler`).
3. You need an Android emulator to run the automated tests on. We recommend an Android 5.1 emulator (smaller resolutions run a bit faster, so a Nexus 4 might be better than a Nexus 9).
4. Start the Appium server by executing: `appium &`
5. The tests are ran using a built apk of the project. Make sure you have the latest built code by executing `gradlew assembleDebug`.

To run the tests, execute the command `cucumber` in the root of this project. If everything is set up correctly, you should see a lot of activity from the Appium server, and a couple seconds later the emulator should launch the app for the first tests.

As of now there are just a few simple tests, but we plan to increase their number.