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

set(GO_OS    "android")
set(GO_ARCH  "${NDK_GO_ARCH_MAP_${ANDROID_ARCH_NAME}}")

# The env var COMMON_VERSION comes from the build.gradle
set(INTERMEDIARY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/eduvpn-common/lib")
set(INTERMEDIARY_DEFAULT_HEADER "${INTERMEDIARY_DIR}/libeduvpn_common-${COMMON_VERSION}.h")
set(INTERMEDIARY_HEADER "${INTERMEDIARY_DIR}/eduvpn_common.h")
set(INTERMEDIARY_FILE "libeduvpn_common-${COMMON_VERSION}.so")
set(INTERMEDIARY_FILE_PATH "${INTERMEDIARY_DIR}/${INTERMEDIARY_FILE}")
set(OUTPUT_FILE "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}/${INTERMEDIARY_FILE}")
set(CLANG_FLAGS "-arch ${GO_ARCH} --target=${ANDROID_LLVM_TRIPLE} --gcc-toolchain=${ANDROID_TOOLCHAIN_ROOT} --sysroot=${ANDROID_SYSROOT}")

# Remove -mthumb
string(REPLACE " -mthumb" "" CMAKE_C_FLAGS_NO_THUMB ${CMAKE_C_FLAGS})
string(REPLACE " -mthumb" "" CMAKE_CXX_FLAGS_NO_THUMB ${CMAKE_CXX_FLAGS})

# Compile from Go to C
add_custom_command(
        OUTPUT ${INTERMEDIARY_FILE_PATH} ${INTERMEDIARY_DEFAULT_HEADER}
        WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/eduvpn-common"
        COMMENT "Building shared library for ${ANDROID_LLVM_TRIPLE}"
        VERBATIM
        COMMAND ${CMAKE_COMMAND} -E env
		CGO_ENABLED=1
		GOOS=${GO_OS}
		GOARCH=${GO_ARCH}
		CC=${ANDROID_C_COMPILER}
		CXX=${ANDROID_CXX_COMPILER}
		CGO_CPPFLAGS=${CLANG_FLAGS}\ --target=${ANDROID_LLVM_TRIPLE}
		CGO_CFLAGS=${CLANG_FLAGS}\ ${CMAKE_C_FLAGS_NO_THUMB}\ -Wno-unused-command-line-argument
		CGO_CXXFLAGS=${ClANG_FLAGS}\ ${CMAKE_CXX_FLAGS_NO_THUMB}
		CGO_LDFLAGS=${CLANG_FLAGS}\ ${CMAKE_SHARED_LINKER_FLAGS}\ -Wl,-soname,${INTERMEDIARY_FILE}
	go build -o "${INTERMEDIARY_FILE_PATH}" -buildmode=c-shared ./exports
)
# Copy from intermediary to output file
add_custom_command(
        OUTPUT ${OUTPUT_FILE} ${INTERMEDIARY_HEADER}
        DEPENDS ${INTERMEDIARY_FILE_PATH} ${INTERMEDIARY_DEFAULT_HEADER}
        COMMAND cp ${INTERMEDIARY_FILE_PATH} ${OUTPUT_FILE}
        COMMAND cp ${INTERMEDIARY_DEFAULT_HEADER} ${INTERMEDIARY_HEADER}
)
# Set the output file on the target
add_custom_target(libeduvpn_common DEPENDS ${OUTPUT_FILE} ${INTERMEDIARY_HEADER})
# Build the JNI code which will interface the common library to the shared code
add_library(eduvpn_common-wrapper SHARED
        ../src/main/cpp/jni.cpp ${INTERMEDIARY_HEADER}
        )
target_include_directories(eduvpn_common-wrapper PUBLIC ${INTERMEDIARY_DIR})
add_dependencies(eduvpn_common-wrapper libeduvpn_common)

# The wrapper library needs to be linked to the common library and also Android
target_link_libraries(eduvpn_common-wrapper
        ${OUTPUT_FILE}
        android
        log
        )
