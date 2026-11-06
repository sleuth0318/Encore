Build App (README)

1)Install Java latest
---
https://learn.microsoft.com/en-us/java/openjdk/download

new directory => ~Java/jdk-21 (extract here)
confirm java patrh => export JAVA_HOME=/home/fedora/Java/jdk-21
confirm jdk-21 => $JAVA_HOME/bin/javac -version

2) Install Android Sdk command line tools
---
https://developer.android.com/studio

new directory => ~Android/Sdk/cmdline-tools (extract here)
confirm path =>
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

3) Accept licenses => yes | sdkmanager --licenses  
---

4) Install required tools => sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
---

5) Build
---
./gradlew assembleDebug
./gradlew assembleRelease
