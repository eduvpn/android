# Tag a Release

Walk through the following steps when making a new release:

* Update `CHANGES.md` to make sure all changes are recorded for the new release 
  including the release date, e.g.: `3.2.0 (2032-12-22)`;
* Increment the `versionCode` and set `versionName` to the correct version tag 
  in `app/build.gradle`;
* Copy the entries from the `CHANGES.md` file you made also to 
  `fastlane/metadata/android/en-US/changelogs/$versionCode.txt`;
* Update the `build_app_git.sh`, `build_app_tar.sh`, `create_release_tar.sh`, 
  and if necessary `builder_setup.sh`;
* Create an "annotated" tag with e.g. `git tag 3.2.0 -a -m '3.2.0'`;
* Push the tag, e.g.: `git push 3.2.0`;
* Create a release with `create_release_tar.sh`
