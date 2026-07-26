#import <Foundation/Foundation.h>
#import <Metal/Metal.h>
#include <jni.h>

static id<MTLDevice> device_from_handle(jlong handle) {
    return (__bridge id<MTLDevice>)(void *)(intptr_t)handle;
}

static jstring java_string(JNIEnv *environment, NSString *value) {
    if (value == nil) {
        return NULL;
    }
    return (*environment)->NewStringUTF(environment, value.UTF8String);
}

JNIEXPORT jlong JNICALL
Java_com_nologo_math2_metal_NativeMetalRuntime_openDefaultDevice(
        JNIEnv *environment, jclass runtime_class) {
    (void)environment;
    (void)runtime_class;
    @autoreleasepool {
        id<MTLDevice> device = MTLCreateSystemDefaultDevice();
        if (device == nil) {
            return 0;
        }
        return (jlong)(intptr_t)(__bridge_retained void *)device;
    }
}

JNIEXPORT jstring JNICALL
Java_com_nologo_math2_metal_NativeMetalRuntime_registryId(
        JNIEnv *environment, jclass runtime_class, jlong handle) {
    (void)runtime_class;
    @autoreleasepool {
        id<MTLDevice> device = device_from_handle(handle);
        NSString *registry_id = [NSString stringWithFormat:@"%llu", device.registryID];
        return java_string(environment, registry_id);
    }
}

JNIEXPORT jstring JNICALL
Java_com_nologo_math2_metal_NativeMetalRuntime_deviceName(
        JNIEnv *environment, jclass runtime_class, jlong handle) {
    (void)runtime_class;
    @autoreleasepool {
        return java_string(environment, device_from_handle(handle).name);
    }
}

JNIEXPORT jlong JNICALL
Java_com_nologo_math2_metal_NativeMetalRuntime_memoryLimitBytes(
        JNIEnv *environment, jclass runtime_class, jlong handle) {
    (void)environment;
    (void)runtime_class;
    @autoreleasepool {
        return (jlong)device_from_handle(handle).recommendedMaxWorkingSetSize;
    }
}

JNIEXPORT void JNICALL
Java_com_nologo_math2_metal_NativeMetalRuntime_closeDevice(
        JNIEnv *environment, jclass runtime_class, jlong handle) {
    (void)environment;
    (void)runtime_class;
    if (handle != 0) {
        CFBridgingRelease((void *)(intptr_t)handle);
    }
}
