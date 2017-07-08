Music Center for Wear
================================

Application for customizable music control from Android Wear devices with support for additional watch's buttons and digital crown.

Project is still work in progress (not ready a for release yet).

## Building

### Prerequisites

* [Android Studio 3.0 or newer](https://developer.android.com/studio/preview/index.html)
* [Protobuf compiler](https://github.com/google/protobuf/releases/latest)
* [protoc-gen-javalite plugin](https://github.com/google/protobuf/releases/tag/v3.0.0)

### Build process

#### 1. Pull the repo
#### 2. Pull the submodules

Run command `git submodule update --init`

#### 3. Compile protocol buffers

Run command `protoc -I common/src/main --javalite_out=common/src/main/java common/src/main/proto/*.proto` in the project root.

Windows users can use provided `update_protobuf.bat` file if both `protoc.exe` and `protoc-gen-javalite.exe` are in path

(This is supposed to be completely automated step, but protocol buffers [do not support new Android Studio yet](https://github.com/google/protobuf-gradle-plugin/issues/129))

#### 4. Open the project in the Android Studio and wait for its dependencies to resolve