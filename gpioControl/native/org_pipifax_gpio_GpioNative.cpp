
#include <jni.h>
/* Implementation for class org_pipifax_gpio_GpioNative */

#ifndef _Included_org_pipifax_gpio_GpioNative
#define _Included_org_pipifax_gpio_GpioNative


jint init(jint debug)
{
    return 1;
}

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    init
 * Signature: (Z)I
 */
JNIEXPORT jint JNICALL Java_org_pipifax_gpio_GpioNative_init__Z
  (JNIEnv * env, jobject obj, jboolean debug)
{
  return init(debug);
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    init
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_pipifax_gpio_GpioNative_init__
  (JNIEnv * env, jobject obj)
{
    return init(false);
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    writeDigital
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_writeDigital
  (JNIEnv * env, jobject obj, jint pin, jboolean value)
{
    // not yet implemented
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    close
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_pipifax_gpio_GpioNative_close
  (JNIEnv * env, jobject obj)
{
    return 1;
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    gpioFsel
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_gpioFsel
  (JNIEnv * env, jobject obj, jint pin, jint state)
{
    // not implemented yet
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    gpioWriteMulti
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_gpioWriteMulti
  (JNIEnv * env, jobject obj, jint pinMask, jint valueMask)
{
    // not implemented yet
}

#ifdef __cplusplus
}
#endif
#endif
