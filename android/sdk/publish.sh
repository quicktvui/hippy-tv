#!/bin/zsh

ver=1.8.80-beta777

# Debug
./gradlew assembleDebug -PV8_RELEASE= -PINCLUDE_ABI_ARMEABI_V7A=true -PENABLE_SO_DOWNLOAD=false
./gradlew publishToMavenLocal -PV8_RELEASE= -PVERSION_NAME="$ver"

# Release
#./gradlew assembleRelease
#./gradlew publishToMavenLocal -PVERSION_NAME="$ver"
