#!/bin/sh
# Gradle start up script for UNIX (open project in Android Studio to sync wrapper if gradle-wrapper.jar is absent)
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
  fi
  echo "Open NoorAlIslamAndroid in Android Studio or install Gradle 8.7 to run Build APK."
  exit 1
fi
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" -Xmx64m -Xms64m -jar "$WRAPPER_JAR" "$@"
