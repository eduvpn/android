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