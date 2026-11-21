#!/bin/bash
set -e
AJ=/usr/lib/android-sdk/platforms/android-23/android.jar
rm -rf build && mkdir -p build/classes build/gen
aapt package -f -M AndroidManifest.xml -S res -A assets -I $AJ -J build/gen -F build/app.unsigned.apk
javac -nowarn --release 8 -cp /home/claude/dl/a34.jar -d build/classes $(find src build/gen -name '*.java') 2>&1 | grep -v "^warning\|bootstrap\|^1 warning" || true
dalvik-exchange --dex --min-sdk-version=24 --output=build/classes.dex build/classes
(cd build && aapt add app.unsigned.apk classes.dex >/dev/null)
zipalign -f -p 4 build/app.unsigned.apk build/app.aligned.apk
[ -f nutriscan.keystore ] || keytool -genkeypair -keystore nutriscan.keystore -alias nutriscan -keyalg RSA -keysize 2048 -validity 10000 -storepass nutriscan -keypass nutriscan -dname "CN=NutriScan, O=Private, C=DE" 2>&1 | grep -v JAVA_TOOL
apksigner sign --ks nutriscan.keystore --ks-pass pass:nutriscan --key-pass pass:nutriscan --min-sdk-version 24 --out build/NutriScan.apk build/app.aligned.apk
apksigner verify --verbose build/NutriScan.apk | head -5
ls -la build/NutriScan.apk
