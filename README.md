## Introduction

Node4crosswalk is an html5 runtime which supports nodejs, based on [Crosswalk](https://crosswalk-project.org/)/Chromium/Blink.


## How to
You need to install extra prerequisites and depot tools covered in [building Chrome for Android](https://www.chromium.org/developers/how-tos/android-build-instructions).
### Download code ###
* `export XWALK_OS_ANDROID=1`
* `gclient config --name=src/xwalk http://github.com/axinging/crosswalk.git@node4crosswalk`
* `gclient sync`

### Build ###
*  Create chromium.gyp_env at top directory:
{ 'GYP_DEFINES': 'OS=android target_arch=arm use_sysroot=0',}
* `cd src`
* `python xwalk/gyp_xwalk`
* `ninja -C out/Debug xwalk_core_shell_apk`
* `adb install -r out/Debug/apks/XWalkCoreShell.apk`

### Run ###
*  Before run, push src/xwalk/nodejs/init.js to /data/local/tmp/node/resources/init.js.

### Examples ###
*  Node4corsswalk examples: https://github.com/axinging/node4crosswalk_demo.

For detailed build instructions, please follow: [Building Crosswalk for Android](https://crosswalk-project.org/contribute/building_crosswalk.html).

Check out crosswallk's [wiki](http://crosswalk-project.org/#wiki).

## License

Node4crosswalk's code uses the 3-clause BSD license, see our `LICENSE` file.
