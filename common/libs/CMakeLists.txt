cmake_minimum_required(VERSION 3.4.1)
project("eduvpn-common")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")

# Android -> Go architecture map
set(NDK_GO_ARCH_MAP_x86     "386")
set(NDK_GO_ARCH_MAP_x86_64  "amd64")
set(NDK_GO_ARCH_MAP_arm     "arm")
set(NDK_GO_ARCH_MAP_arm64   "arm64")
set(NDK_GO_ARCH_MAP_mips    "mipsx")
set(NDK_GO_ARCH_MAP_mips64  "mips64x")

# The env var COMMON_VERSION comes from the build.gradle
set(INTERMEDIARY_FILE "${CMAKE_CURRENT_SOURCE_DIR}/eduvpn-common/exports/lib/android/libeduvpn_common-${COMMON_VERSION}.so")
set(OUTPUT_FILE "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/libeduvpn_common-${COMMON_VERSION}.so")

find_program(MAKE_EXECUTABLE
    NAMES gmake mingw32-make make
    NAMES_PER_DIR
    DOC "GNU Make"
    REQUIRED
)

# Compile from Go to C
add_custom_command(
        OUTPUT ${INTERMEDIARY_FILE}
        WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/eduvpn-common/exports"
        COMMENT "Building shared library for ${ANDROID_LLVM_TRIPLE}"
        VERBATIM
        COMMAND ${MAKE_EXECUTABLE}
        GOOS=android GOARCH=${GOARCH}
        CC=${ANDROID_C_COMPILER} CXX=${ANDROID_CXX_COMPILER}
        CGO_CPPFLAGS=--target=${ANDROID_LLVM_TRIPLE} CGO_CFLAGS=${CMAKE_C_FLAGS} CGO_CXXFLAGS=${CMAKE_CXX_FLAGS}
        CGO_LDFLAGS=${CMAKE_SHARED_LINKER_FLAGS}\ --target=${ANDROID_LLVM_TRIPLE}
)
# Copy from intermediary to output file
add_custom_command(
        OUTPUT ${OUTPUT_FILE}
        DEPENDS ${INTERMEDIARY_FILE}
        COMMAND cp ${INTERMEDIARY_FILE} ${OUTPUT_FILE}
)
# Set the output file on the target
add_custom_target(libeduvpn_common DEPENDS ${OUTPUT_FILE})

# Build the JNI code which will interface the common library to the shared code
add_library(eduvpn_common-wrapper SHARED
        ../src/main/cpp/jni.cpp
        )
add_dependencies(eduvpn_common-wrapper libeduvpn_common)

# The wrapper library needs to be linked to the common library and also Android
target_link_libraries(eduvpn_common-wrapper
        ${OUTPUT_FILE}
        android
        log
        )