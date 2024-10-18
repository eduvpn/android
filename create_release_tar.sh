#!/bin/sh

GIT_REPO=https://codeberg.org/eduVPN/android
GIT_TAG=3.3.4
#GIT_TAG=master

###############################################################################
# CLONE
###############################################################################

(
    mkdir -p release
    cd release || exit

    git clone --recursive -b ${GIT_TAG} ${GIT_REPO} eduvpn-android-${GIT_TAG}

    (
        cd eduvpn-android-${GIT_TAG} || exit
        # remove all ".git" folders
        find . -type d -name ".git" | xargs rm -rf
    )

    tar -cJf eduvpn-android-${GIT_TAG}.tar.xz eduvpn-android-${GIT_TAG}
    rm -rf eduvpn-android-${GIT_TAG}
    minisign -Sm eduvpn-android-${GIT_TAG}.tar.xz
)
