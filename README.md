Music Center for Wear
================================

Application for customizable music control from Android Wear devices with support for additional watch's buttons and digital crown.

Application is currently in beta testing.

[![Play Store link](https://play.google.com/intl/en_us/badges/images/badge_new.png)](https://play.google.com/apps/testing/com.matejdro.wearmusiccenter)


## Building

### Prerequisites

* [Android Studio 3.0 or newer](https://developer.android.com/studio/preview/index.html)

### Build process

#### 1. Pull the repo
#### 2. Pull the submodules

Run command `git submodule update --init`

#### 3. Open the project in the Android Studio and wait for its dependencies to resolve

#### 4. Either comment out Fabric plugin from `/mobile/build.gradle` or [generate your own Fabric API key](https://docs.fabric.io/android/fabric/settings/api-keys.html)