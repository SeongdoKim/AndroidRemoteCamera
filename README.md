# AndroidRemoteCamera
This repository is an Android application to stream the live images acquired by the camera of the device, to other machine. To compile and run this code, you need to install [Android studio](https://developer.android.com/studio/index.html) first.

# Instruction
After you compile this source codes and install it to your Android device, you can connect your device to another machine. To make a connection, you must know the IP address of the target machine. If the target machine uses a static IP address, you would not have any problem. Otherwise, to make a connection, your device, and the target machine should be on the same network.

### Change the size of streaming image
By default, the size of streaming is set to the minimum available size. To change the size of the image to be streamed, you should modify the line 607 in *CameraPreviewFragment.java*.
