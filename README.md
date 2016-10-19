EduVPN PoC
==========

This application is a proof of concept which showcases a simple flow where the user can log in, and download an OpenVPN configuration.
After the configuration has been downloaded and validated, it will immediately initialize a connection with the server.

### Specification

The specification for the final version of this app can be found [here](https://github.com/eduvpn/documentation/tree/master/app).
This PoC is merely a skeleton which shows that downloading a profile and connecting to an OpenVPN server is possible.

### Building the project

Requirements:

* JDK 1.8 or higher
* Android SDK Platform 24 (7.0 Nougat)
* Android Studio 2.1.3 (if higher, the Gradle versions and the Android Gradle plugin might need version updates in the build files)

Instructions:

1. Clone this repository
2. Make sure you check out all the submodules (recursively!)
3. Import the project and build it

### Updating the OpenVPN implementation version

For the forked OpenVPN project:

1. Clone our fork of the ics-openvpn repository
2. Fetch the latest changes from upstream: `git fetch schwabe`
3. Rebase our changes upon the upstream changes: `git rebase schwabe/master`
4. If there are any conflicts resolve them, and continue rebasing
5. When finished, push your changes to the fork: `git push origin master -f` (use this command carefully since it is a force push)

For the application project:

1. Clone this repository
2. For the native libraries, go to the [apk repository](http://plai.de/android/), and download the apk with the latest date.
3. Rename the downloaded apk to a zip file, and open it with your archive manager. Unzip the `assets/` and `/lib` directories.
4. If the `assets/` directory contains an .html file, remove it.
5. Move the two extracted directories inside the project folder `android/app/src/main/ovpnlibs`, overwriting all files.
6. Update the submodule: `cd ics-openvpn/ && git pull origin master` 

You should now have the latest code of the upstream project. It might happen that the native libs and the source code are out of sync.
In this case you can do two things:

* Build the native sources yourself, by following [this guide](https://github.com/schwabe/ics-openvpn/blob/master/doc/README.txt). This will surely have the latest sources, but it requires the Android NDK and currently does not work on Windows.

* Revert the upstream code to the same tag as the apk version. This means that you will be on an older code, but at least it is the same version as in Google Play, so less prone to bugs.

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