
#include <jni.h>
#include <bcm2835.h>
#include <stdint.h>
#include <pthread.h>

/* Implementation for class org_pipifax_gpio_GpioNative */

#ifndef _Included_org_pipifax_gpio_GpioNative
#define _Included_org_pipifax_gpio_GpioNative

struct pwmCmd_t {
    uint8_t pin;
    bool enabled;
};

struct pwmCtrl_t {
    uint8_t pin;
    bool enabled;
    unsigned int commandCount;
    unsigned int curIndex;
    pwmCmd_t *commands;
};

volatile uint8_t pinCount = 6;
volatile uint8_t pins[] = {4,17,18,27, 22,25,4,4, 4,4,4,4, 4,4,4,4};
volatile pwmCtrl_t controls[16];

pthread_mutex_t pwmLoopMutex = PTHREAD_MUTEX_INITIALIZER;
volatile unsigned int pwmJiffie = 5;
volatile unsigned int pwmPeriod = 100;
volatile uint32_t pwmCtrlVersion;


volatile bool stopping = false
volatile bool stopped = false

jboolean init(jboolean debug)
{

    if (debug) bcm2835_set_debug(1);
    else bcm2835_set_debug(0);

    return bcm2835_init();
}

void pwmLoop()
{
    pthread_mutex_lock(&pwmLoopMutex);
    stopping = false;
    stopped = false;
    uint8_t curPinCount = pinCount;
    uint32_t curVersion = pwmCtrlVersion;
    pthread_mutex_unlock(&pwmLoopMutex);
    pwmCtrl_t *localControls;

    while(true) {
        pthread_mutex_lock(&pwmLoopMutex);
        bool exit = stopping;
        if (!exit) {
            uint32_t newVersion = pwmCtrlVersion;
            if(newVersion > curVersion) {
                if (localControls != null) {
                    delete [] localControls;
                }
                curPinCount = pinCount
                localControls = new pwmCtrl_t[curPinCount];
                for (int i = 0; i < curPinCount; i++) {
                    localControls[i] = controls[i];
                    if (localControls[i].commands)
                }
                curVersion = newVersion;
            }
        }
        pthread_mutex_unlock(&pwmLoopMutex);

        bcm2835_delay(pwmJiffie * pwmPeriod);
    }

}

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    init
 * Signature: (Z)I
 */
JNIEXPORT jboolean JNICALL Java_org_pipifax_gpio_GpioNative_init__Z
  (JNIEnv * env, jobject obj, jboolean debug)
{
  return init(debug);
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    init
 * Signature: ()I
 */
JNIEXPORT jboolean JNICALL Java_org_pipifax_gpio_GpioNative_init__
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
    bcm2835_gpio_write(pin, value ? 1 : 0);
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    close
 * Signature: ()I
 */
JNIEXPORT jboolean JNICALL Java_org_pipifax_gpio_GpioNative_close
  (JNIEnv * env, jobject obj)
{
    return bcm2835_close() ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    gpioFsel
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_gpioFsel
  (JNIEnv * env, jobject obj, jint pin, jint state)
{
    if (pin < pinCount) {
        uint8_t port = pins[pin]
        bcm2835_gpio_fsel(port, state);
    }
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    setPWMParams
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_setPWMParams
  (JNIEnv * env, jobject obj, jint jiffie, jint period)
{
    pwmJiffie = (unsigned int) jiffie;
    pwmPeriod = (unsigned int) period;
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    writeAnalog
 * Signature: (IF)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_writeAnalog
  (JNIEnv * env, jobject obj, jint pin, jfloat value)
{
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    writeBlink
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_writeBlink
  (JNIEnv * env, jobject obj, jint pin, jint delay)
{
}

/*
* Class:     org_pipifax_gpio_GpioNative
* Method:    writeWobble
* Signature: (I)V
*/
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_writeWobble
    (JNIEnv * env, jobject obj, jint pin)
{
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

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    initPinMap
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_initPinMap
  (JNIEnv * env, jobject obj, jbyteArray pinNumbers)
{
    jint length = env->GetArrayLength(pinNumbers)
    if (length < 16) {
        pinCount = length
        jbyte *elems = env->GetBooleanArrayElements(pinNumbers, null)
        for (i = 0; i < length; i++) {
            pins[i] = elems[i]
        }
        env->ReleaseByteArrayElements(pinNumbers, elems, JNI_ABORT)
    }
}

#ifdef __cplusplus
}
#endif
#endif
