###############################################################################
# ANDROID-REBUILDS SDK CONFIGURATION
###############################################################################

AR_SDK_DIR=${HOME}/android-rebuilds-sdk

# Android-Rebuilds F-Droid Mirror, will have to be changed to something hosted by SURF or the original AR repo considering the NDK is already unavailable
AR_SDK_URL=https://mirror.f-droid.org/android-free/repository/

# Android-Rebuilds NDK mirror, temporary host because F-Droid does not host NDK version R21
AR_NDK_URL=https://aaio.eu/ndk/
AR_NDK_FILE=android-ndk-0-linux-x86_64.tar.bz2

declare -a arr=(
    "sdk-repo-linux-tools-26.1.1.zip" # Includes sdkmanager
    "sdk-repo-linux-platforms-eng.11.0.0_r27.zip" # To compile ics-openvpn
    "sdk-repo-linux-platforms-eng.10.0.0_r36.zip" # To compile eduvpn
    "sdk-repo-linux-platform-tools-eng.10.0.0_r14.zip" # Contains tools like adb and fastboot
    "sdk-repo-linux-build-tools-eng.10.0.0_r14.zip" # Contains tools like apksigner
)

# Creating necessary folder structure
(
    mkdir -p "${AR_SDK_DIR}" "${AR_SDK_DIR}"/platforms "${AR_SDK_DIR}"/build-tools "${AR_SDK_DIR}"/ndk "${AR_SDK_DIR}"/ndk/21.4.0
)

# Downloading and extracting SDK
(
    cd "${AR_SDK_DIR}" || exit
    for i in "${arr[@]}"
    do
        echo "Downloading $i"
        curl -L -O ${AR_SDK_URL}/$i
        echo "Unzipping $i"
        unzip -o -q $i -d ${AR_SDK_DIR} | pv -l > /dev/null
        rm $i
        # Some of these zips need to be either placed in subfolders or have to be
        # renamed due to gradle warnings
        if [ $i = sdk-repo-linux-tools-26.1.1.zip ]
        then
            mv tools/ cmdline-tools/
        elif [ $i = sdk-repo-linux-build-tools-eng.10.0.0_r14.zip ]
        then
            mv android-10/ build-tools/29.0.2
        elif [ $i = sdk-repo-linux-platforms-eng.10.0.0_r36.zip ]
        then
            mv android-10/ platforms/android-29
        elif [ $i = sdk-repo-linux-platforms-eng.11.0.0_r27.zip ]
        then
            mv android-11/ platforms/android-30
        fi
        echo "Content of $i in place"
        echo ""
    done
)

# Downloading and extracting NDK
(
    echo "Downloading ${AR_NDK_FILE}. This might take a while depending on your connection"
    curl -L -O ${AR_NDK_URL}/${AR_NDK_FILE}
    echo "Extracting ${AR_NDK_FILE}. This might take a while depending on your system."
    pv ${AR_NDK_FILE} | tar xj
    rm ${AR_NDK_FILE}
    cp -rlf android-ndk-r21e/* "${AR_SDK_DIR}"/ndk/21.4.0/
    rm -rf android-ndk-r21e/
    echo "Content of ${AR_NDK_FILE} in place"
    echo ""
)