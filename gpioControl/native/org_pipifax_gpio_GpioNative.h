/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_pipifax_gpio_GpioNative */

#ifndef _Included_org_pipifax_gpio_GpioNative
#define _Included_org_pipifax_gpio_GpioNative
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    init
 * Signature: (Z)I
 */
JNIEXPORT jint JNICALL Java_org_pipifax_gpio_GpioNative_init__Z
  (JNIEnv *, jobject, jboolean);

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    init
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_pipifax_gpio_GpioNative_init__
  (JNIEnv *, jobject);

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    writeDigital
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_writeDigital
  (JNIEnv *, jobject, jint, jboolean);

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    close
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_pipifax_gpio_GpioNative_close
  (JNIEnv *, jobject);

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    gpioFsel
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_gpioFsel
  (JNIEnv *, jobject, jint, jint);

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    gpioWriteMulti
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_gpioWriteMulti
  (JNIEnv *, jobject, jint, jint);

#ifdef __cplusplus
}
#endif
#endif