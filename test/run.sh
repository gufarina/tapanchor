#!/bin/sh
# Testa o dominio puro com o JDK, sem Android.
cd "$(dirname "$0")/.." && rm -rf build/test && mkdir -p build/test &&
javac -d build/test app/src/main/java/com/tapanchor/app/domain/*.java test/TapGuardTest.java &&
java -cp build/test TapGuardTest
