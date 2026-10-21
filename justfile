# StarClone Android — build commands

# List all available commands
default:
    @just --list

# Compile Kotlin sources (debug)
build-kotlin:
    ./gradlew app:compilePlaystoreDebugKotlin

# Compile Java sources (debug)
build-java:
    ./gradlew app:compilePlaystoreDebugJavaWithJavac

# Compile both Kotlin and Java
build:
    ./gradlew app:compilePlaystoreDebugKotlin app:compilePlaystoreDebugJavaWithJavac

# Assemble per-ABI debug APKs
apk:
    ./gradlew app:assemblePlaystoreDebug

# Install debug APK matching connected device's ABI (~64MB instead of ~174MB universal)
install:
    #!/usr/bin/env bash
    set -euo pipefail
    ABI=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')
    APK=$(find app/build/outputs/apk/playstore/debug -name "*-${ABI}-debug.apk" | head -1)
    if [ -z "$APK" ]; then
        echo "No APK found for ABI $ABI — run 'just apk' first"
        exit 1
    fi
    echo "Installing $APK ($ABI)"
    adb install -r "$APK"

# Build and install in one step
run: apk install

# Clean build artifacts
clean:
    ./gradlew clean
