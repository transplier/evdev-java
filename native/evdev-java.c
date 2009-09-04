#include <stdint.h>

#include <linux/input.h>

#include <string.h>
#include <fcntl.h>
#include <unistd.h>

#include <jni.h>
#include "com_dgis_input_evdev_EventDevice.h"

/*
 * Class:     com_dgis_input_evdev_EventDevice
 * Method:    ioctlGetID
 * Signature: (Ljava/lang/String;[S)V
 */
JNIEXPORT jboolean JNICALL Java_com_dgis_input_evdev_EventDevice_ioctlGetID
	(JNIEnv *env, jobject obj, jstring device_name, jshortArray out) {
  
	/* Get C references to Java objects */
	const char* device_name_str = (*env)->GetStringUTFChars (env, device_name, NULL);
	short* id = (*env)->GetShortArrayElements (env, out, NULL);
  
	/* Do the ioctl */
	int fd, retval;
	if ((fd = open(device_name_str, O_RDONLY)) < 0) {
		retval=0;
	} else {
		ioctl(fd, EVIOCGID, id);
		close(fd);
		retval=1;
	}
  
	/* Release C references to Java objects */
	(*env)->ReleaseShortArrayElements (env, out, id, 0);
	(*env)->ReleaseStringUTFChars (env, device_name, device_name_str);
  
	return retval;
}

/*
 * Class:     com_dgis_input_evdev_EventDevice
 * Method:    ioctlGetEvdevVersion
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_com_dgis_input_evdev_EventDevice_ioctlGetEvdevVersion
	(JNIEnv *env, jobject obj, jstring device_name) {
  
	/* Get C references to Java objects */
	const char* device_name_str = (*env)->GetStringUTFChars (env, device_name, NULL);

	/* Do the ioctl */
	int fd, version;
	if ((fd = open(device_name_str, O_RDONLY)) < 0) {
		version = 0;
	} else {
		if (ioctl(fd, EVIOCGVERSION, &version)) {
			version = 0;
		}
		close(fd);
	}

	/* Release C references to Java objects */
	(*env)->ReleaseStringUTFChars (env, device_name, device_name_str);

	return version;
}

/*
 * Class:     com_dgis_input_evdev_EventDevice
 * Method:    ioctlGetDeviceName
 * Signature: ([BI)V
 */
JNIEXPORT jboolean JNICALL Java_com_dgis_input_evdev_EventDevice_ioctlGetDeviceName
	(JNIEnv *env, jobject obj, jstring device_name, jbyteArray name) {

	/* Get C references to Java objects */
	const char* device_name_str = (*env)->GetStringUTFChars (env, device_name, NULL);
	signed char* name_str = (*env)->GetByteArrayElements (env, name, NULL);

	/* Do the ioctl */
	int fd, retval;
	if ((fd = open(device_name_str, O_RDONLY)) < 0) {
		retval=0;
	} else {
		ioctl(fd, EVIOCGNAME((*env)->GetArrayLength(env, name)), name_str);
		close(fd);
		retval=1;
	}

	/* Release C references to Java objects */
	(*env)->ReleaseByteArrayElements (env, name, name_str, 0);
	(*env)->ReleaseStringUTFChars (env, device_name, device_name_str);

	return retval;
}

/*
 * Class:     com_dgis_input_evdev_EventDevice
 * Method:    ioctrlEVIOCGBIT
 * Signature: (Ljava/lang/String;[J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_dgis_input_evdev_EventDevice_ioctlEVIOCGBIT
	(JNIEnv *env, jobject obj, jstring device_name, jlongArray out, jint start, jint stop) {
	/* Get C references to Java objects */
	const char* device_name_str = (*env)->GetStringUTFChars (env, device_name, NULL);
	long* resp = (*env)->GetLongArrayElements (env, out, NULL);

	/* Do the ioctl */
	int fd, retval;
	if ((fd = open(device_name_str, O_RDONLY)) < 0) {
		retval=0;
	} else {
		ioctl(fd, EVIOCGBIT(start, stop), resp);
		close(fd);
		retval=1;
	}

	/* Release C references to Java objects */
	(*env)->ReleaseLongArrayElements (env, out, resp, 0);
	(*env)->ReleaseStringUTFChars (env, device_name, device_name_str);

	return retval;
}

/*
 * Class:     com_dgis_input_evdev_EventDevice
 * Method:    ioctlEVIOCGABS
 * Signature: (Ljava/lang/String;[II)Z
 */
JNIEXPORT jboolean JNICALL Java_com_dgis_input_evdev_EventDevice_ioctlEVIOCGABS
	(JNIEnv *env, jobject obj, jstring device_name, jintArray out, jint axis) {
	
	if((*env)->GetArrayLength(env, out) < 5) return 0;
	
	/* Get C references to Java objects */
	const char* device_name_str = (*env)->GetStringUTFChars (env, device_name, NULL);
	int* resp = (*env)->GetIntArrayElements (env, out, NULL);

	/* Do the ioctl */
	int fd, retval;
	if ((fd = open(device_name_str, O_RDONLY)) < 0) {
		retval=0;
	} else {
		ioctl(fd, EVIOCGABS(axis), resp);
		close(fd);
		retval=1;
	}

	/* Release C references to Java objects */
	(*env)->ReleaseIntArrayElements (env, out, resp, 0);
	(*env)->ReleaseStringUTFChars (env, device_name, device_name_str);

	return retval;
}
