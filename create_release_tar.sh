#!/bin/sh

GIT_REPO=https://github.com/eduvpn/android
GIT_TAG=2.0.0
#GIT_TAG=master

PROJECT_DIR=${HOME}/Projects
APP_DIR=${PROJECT_DIR}/eduvpn-android-${GIT_TAG}

###############################################################################
# CLONE
###############################################################################

(
    mkdir -p "${PROJECT_DIR}"
    cd "${PROJECT_DIR}" || exit

    git clone --recursive -b ${GIT_TAG} ${GIT_REPO} "${APP_DIR}"

    (
        cd "${APP_DIR}" || exit

        # remove all ".git" folders
        find . -type d -name ".git" | xargs rm -rf
    )

    tar -cJf eduvpn-android-${GIT_TAG}.tar.xz eduvpn-android-${GIT_TAG}
    minisign -Sm eduvpn-android-${GIT_TAG}.tar.xz
)
