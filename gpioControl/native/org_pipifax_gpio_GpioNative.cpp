
#include <jni.h>
#include <bcm2835.h>
#include <stdint.h>
#include <pthread.h>
#include <iostream>
#include <vector>
#include <errno.h>
#include <string.h>

/* Implementation for class org_pipifax_gpio_GpioNative */

#ifndef _Included_org_pipifax_gpio_GpioNative
#define _Included_org_pipifax_gpio_GpioNative


uint8_t pinCount = 6;
uint8_t pins[] = {4,17,18,27, 22,25,4,4, 4,4,4,4, 4,4,4,4};

uint8_t led2Pin(uint8_t led) {
    if (led < 0 || led >= pinCount) return pins[0];
    return pins[led];
}

class Command {
    uint16_t delay;
    uint8_t pin;
    bool enabled;
public:
    Command() {
        delay = 0;
        pin = 0;
        enabled = false;
    }

    Command(uint16_t d, uint8_t p, bool e) {
        delay = d;
        pin = p;
        enabled = e;
    }

    inline uint16_t getDelay() const {
        return delay;
    }

    inline uint16_t calcRemainingDelay(uint16_t passedDelay) const {
        if (passedDelay > delay) return 0;
        else return delay - passedDelay;
    }


    inline void execute(uint8_t pin) const {
        bcm2835_gpio_write(pin, enabled);
    }

    inline bool getEnabled() const {
        return enabled;
    }

    inline uint8_t getPin() const {
        return pin;
    }

    std::ostream& put(std::ostream& s) const {
        return s << delay << " -> " << enabled;
    }
};

std::ostream& operator<<(std::ostream& s, const Command& o) {
    return o.put(s);
}

class PulseControl {
    std::vector<Command> commands;
    unsigned int curIndex;
    uint16_t remainingDelay;
    uint8_t pin;
    bool enabled;

    void waitForIt() const {
         bcm2835_delay(remainingDelay);
    }

    void nextIndex() {
        curIndex++;
        if (curIndex == commands.size()) curIndex = 0;

        Command& next = commands[curIndex];
        remainingDelay = next.getDelay();
    }

public:
    PulseControl() {
        pin = led2Pin(0);
        enabled = false;
        curIndex = 0;
    }

    PulseControl(const PulseControl& that) {
        pin = that.pin;
        enabled = that.enabled;
        curIndex = that.curIndex;
        commands = that.commands;

        remainingDelay = that.remainingDelay;
    }

    PulseControl(uint8_t led) {
        pin = led2Pin(led);
        enabled = false;
    }

    uint8_t getPin() const {
        return pin;
    }

    bool operator<=(const PulseControl& that) const {
        return remainingDelay <= that.remainingDelay;
    }

    bool operator<(const PulseControl& that) const {
        return remainingDelay < that.remainingDelay;
    }

    void operator=(const PulseControl& that) {
        pin = that.pin;
        enabled = that.enabled;
        curIndex = that.curIndex;
        commands = that.commands;
    }

    uint16_t  getRemainingDelay() const {
        return remainingDelay;
    }

    void updateDelay(uint16_t timePassed) {
        if (timePassed > remainingDelay) {
            std::cout << "Pin " << ((unsigned int)pin) << " overshot delay by " << (timePassed - remainingDelay) << std::endl;
            remainingDelay = 0;
        } else {
//            std::cout << "Pin " << ((unsigned int)pin) << " remainingDelay = " << remainingDelay << " - " << timePassed << std::endl;
            remainingDelay -= timePassed;
        }
    }

    uint16_t executeNextCommand() {
        if (remainingDelay == 0xFFFF) {
            std::cout << "Noop pin " << ((unsigned int)pin) << std::endl;
            return 0;
        }

        uint16_t timeWaited = remainingDelay;
        if (timeWaited > 0) {
            waitForIt();
        }
        Command& cmd = commands[curIndex];

        cmd.execute(pin);

        nextIndex();
//        std::cout << "Pin " << ((unsigned int) pin) << "->" << cmd.getEnabled() << " delay: " << remainingDelay << " return: " << timeWaited << std::endl;
        return timeWaited;
    }

    void enable() {
        if (commands.size() > 0) {
            curIndex = 0;
            Command& next = commands[curIndex];
            remainingDelay = next.getDelay();
            enabled = true;
        }
    }


    bool isEnabled() const {
        return enabled;
    }

    void addCommand(uint16_t delay, bool enabled) {
        commands.push_back(Command(delay, pin, enabled));
    }

    void addCommand(const Command& c) {
       commands.push_back(c);
    }

    void initCommands(unsigned int count, int32_t *values) {
        commands.clear();
        bool on = true;
        for (unsigned int i = 0; i < count; ++i) {
            addCommand((uint16_t)(values[i] & 0x0000FFFF), on);
            on = !on;
        }
    }

    void initCommands(unsigned int count, bool startValue, uint16_t *values) {
        commands.clear();
        bool on = startValue;
        for (unsigned int i = 0; i < count; ++i) {
            addCommand(values[i] & 0xFFFF, on);
            on = !on;
        }
    }

    void initCommands(const std::vector<Command>& newCommands) {
        commands = newCommands;
    }

    const std::vector<Command>& getCommands() const {
        return commands;
    }

    std::ostream& put(std::ostream& s) const {
        s
            << "Pin: " << ((unsigned int) pin)
            << " enabled: " << enabled
            << " remainingDelay: " << remainingDelay
            << " curIndex: " << curIndex
            << " cmds:";

        for (std::vector<Command>::const_iterator it = commands.begin(); it != commands.end(); ++it) {
            s << ", " << *it;
        }
        return s;
    }
};

std::ostream& operator<<(std::ostream& s, const PulseControl& o) {
    return o.put(s);
}

std::vector<std::vector<Command> > programs;

std::vector<PulseControl> controls;

pthread_mutex_t pwmLoopMutex = PTHREAD_MUTEX_INITIALIZER;
pthread_t pwmLoopThread;

volatile uint32_t pwmCtrlVersion;

volatile bool stopping = false;
volatile bool stopped = false;

void * pwmLoopFunction(void *);

void setRealtimePriority(pthread_t thread_id) {
    // struct sched_param is used to store the scheduling priority
    struct sched_param params;
    // We'll set the priority to the maximum.
    params.sched_priority = sched_get_priority_max(SCHED_FIFO);
    std::cout << "Trying to set thread realtime prio = " << params.sched_priority << std::endl;

    // Attempt to set thread real-time priority to the SCHED_FIFO policy
    if (pthread_setschedparam(thread_id, SCHED_FIFO, &params)) {
        // Print the error
        std::cout << "Unsuccessful in setting thread realtime prio" << std::endl;
        return;
    }

    // Now verify the change in thread priority
    int policy = 0;
    if (pthread_getschedparam(thread_id, &policy, &params)) {
        std::cout << "Couldn't retrieve real-time scheduling paramers" << std::endl;
        return;
    }

    // Check the correct policy was applied
    if(policy != SCHED_FIFO) {
        std::cout << "Scheduling is NOT SCHED_FIFO!" << std::endl;
    } else {
        std::cout << "SCHED_FIFO OK" << std::endl;
    }

    // Print thread scheduling priority
    std::cout << "Thread priority is " << params.sched_priority << std::endl;
}

bool startPwmThread() {
    pthread_attr_t attr;
    pthread_attr_init(&attr);
    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

    int failed = pthread_create(&pwmLoopThread, &attr, pwmLoopFunction, NULL);
    if (failed) {
        std::cerr << "Creating pwm thread failed " << failed << std::endl;
        return false;
    } else {
        setRealtimePriority(pwmLoopThread);
        std::cout << "Initializing successful. thread id " << pwmLoopThread << std::endl;
        return true;
    }
}

jboolean init(jboolean debug)
{
    controls.clear();
    for (unsigned int led = 0; led < pinCount; ++led) {
        controls.push_back(PulseControl(led));
    }

    pwmCtrlVersion = 1;

    if (debug) bcm2835_set_debug(1);
    else bcm2835_set_debug(0);

    if (!bcm2835_init()) {
        std::cerr << "Initializing bcm2835 library failed!" << std::endl;
        return JNI_FALSE;
    } else {
        return startPwmThread();
    }
}

jboolean close()
{
    pthread_mutex_lock(&pwmLoopMutex);
    stopping = true;

    pthread_cancel(pwmLoopThread);

    pthread_mutex_unlock(&pwmLoopMutex);

    std::cerr << "Joining pwm thread " << pwmLoopThread << std::endl;
    int rc = pthread_join(pwmLoopThread, NULL);
    if (rc) {
        std::cerr << "Joining pwm thread failed: " << rc << " " << strerror(errno) << std::endl;
    }

    for (unsigned int led = 0; led < pinCount; ++led) {
        bcm2835_gpio_write(led2Pin(led), (uint8_t)0);
        bcm2835_gpio_fsel(led2Pin(led), BCM2835_GPIO_FSEL_INPT);
    }

    return bcm2835_close() ? JNI_TRUE : JNI_FALSE;
}

void copyControlsVec(std::vector<PulseControl>& target) {
    target.clear();
    for (std::vector<PulseControl>::iterator it = controls.begin(); it != controls.end(); ++it) {
        if ((*it).isEnabled()) {
            target.push_back(*it);
        }
    }
}

void pwmLoop()
{
    pthread_mutex_lock(&pwmLoopMutex);
    stopping = false;
    stopped = false;
    uint32_t curVersion = pwmCtrlVersion;

    std::vector<PulseControl> localControls;

    copyControlsVec(localControls);

    pthread_mutex_unlock(&pwmLoopMutex);


    while(true) {
        pthread_mutex_lock(&pwmLoopMutex);
        bool exit = stopping;
        if (!exit) {
            uint32_t newVersion = pwmCtrlVersion;
            if(newVersion > curVersion) {
                std::cout << "newVersion " << newVersion << std::endl;
                copyControlsVec(localControls);
                curVersion = newVersion;

//                std::cout << "localControls: " << std::endl;
//                for (unsigned int i = 0; i < localControls.size(); ++i) {
//                    std::cout << localControls[i] << std::endl;
//                }
            }
        } else {
            return;
        }
        pthread_mutex_unlock(&pwmLoopMutex);


        if (localControls.empty()) {
            bcm2835_delay(1000);
        } else {
            uint16_t totalTimePassed = 0;


            do {
                unsigned int index = 0;
                PulseControl* nextToRun = &localControls[0];
                for (unsigned int i = index + 1; i < localControls.size(); ++i) {
                    if (localControls[i] < *nextToRun) {
                        nextToRun = &localControls[i];
                        index = i;
                    }
                }

//                std::cout << "Executing " << index << " delay: " << nextToRun->getRemainingDelay() << std::endl;

                if (nextToRun->getRemainingDelay() == 0) {
                    nextToRun->executeNextCommand();
                    for (; index < localControls.size(); ++index) {
                        if (localControls[index].getRemainingDelay() == 0) {
//                            std::cout << "Also executing " << index << std::endl;
                            localControls[index].executeNextCommand();
                        }
                    }
                } else {
                    uint16_t timeWaited = nextToRun->executeNextCommand();
                    totalTimePassed += timeWaited;
//                    std::cout << "time waited " << timeWaited << " total time waited " << totalTimePassed << std::endl;

                    for (unsigned int i = 0; i < localControls.size(); ++i) {
                        if (i != index) {
//                            std::cout << "updating delay of " << i << std::endl;
                            localControls[i].updateDelay(timeWaited);
                        }
                    }
                }
            } while (totalTimePassed < 1000);
        }
    }
}

void *pwmLoopFunction(void *) {
    pwmLoop();

    return NULL;
}


extern "C" {

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    initPinMap
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_initPinMap
  (JNIEnv * env, jobject obj, jbyteArray pinNumbers)
{
    jint length = env->GetArrayLength(pinNumbers);
    if (length < 16) {
        pinCount = length;
        jbyte *elems = env->GetByteArrayElements(pinNumbers, NULL);
        pthread_mutex_lock(&pwmLoopMutex);
        for (int i = 0; i < length; i++) {
            pins[i] = elems[i];
        }
        pthread_mutex_unlock(&pwmLoopMutex);
        env->ReleaseByteArrayElements(pinNumbers, elems, JNI_ABORT);
    }
}

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
 * Method:    writeDigital
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_writeDigital
  (JNIEnv * env, jobject obj, jint led, jboolean value)
{
    bcm2835_gpio_write(led2Pin(led), value ? 1 : 0);
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    close
 * Signature: ()I
 */
JNIEXPORT jboolean JNICALL Java_org_pipifax_gpio_GpioNative_close
  (JNIEnv * env, jobject obj)
{
    return close();
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    enableOutput
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_enableOutput
  (JNIEnv *, jobject, jint led, jboolean enabled)
{
   uint8_t mode = enabled ? BCM2835_GPIO_FSEL_OUTP : BCM2835_GPIO_FSEL_INPT;
   uint8_t pin = led2Pin(led);
   std::cout << "pin " << (unsigned) pin << " mode " << (unsigned) mode << std::endl;
   bcm2835_gpio_fsel(pin, mode);
}


/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    createProgram
 * Signature: ([S)I
 */
JNIEXPORT jint JNICALL Java_org_pipifax_gpio_GpioNative_createProgram
  (JNIEnv *env, jobject, jintArray jPgmData)
{
    if (jPgmData == NULL) {
        std::cerr << "jPgmData is null" << std::endl;
        return -1;
    }

    int pgmIndex = -1;

    jint length = env->GetArrayLength(jPgmData);
    if (length > 0) {
        jint *pgmData = env->GetIntArrayElements(jPgmData, NULL);
        PulseControl ctrl;
        ctrl.initCommands(length, pgmData);

        pthread_mutex_lock(&pwmLoopMutex);

        programs.push_back(ctrl.getCommands());
        pgmIndex = programs.size() - 1;

        pthread_mutex_unlock(&pwmLoopMutex);

        env->ReleaseIntArrayElements(jPgmData, pgmData, JNI_ABORT);
    }

    return pgmIndex;
}

/*
 * Class:     org_pipifax_gpio_GpioNative
 * Method:    setProgram
 * Signature: ([II)V
 */
JNIEXPORT void JNICALL Java_org_pipifax_gpio_GpioNative_setProgram
  (JNIEnv *env, jobject, jintArray leds, jint program)
{
    std::cerr << "Starting program " << program << std::endl;

    if (leds == NULL) {
        std::cerr << "leds is null" << std::endl;
        return;
    }
    if (program < 0 || (program >= (jint)programs.size())) {
        std::cerr << "Invalid program id: " << program << std::endl;
        return;
    }

    jint length = env->GetArrayLength(leds);
    if (length > 0) {
        jint *elems = env->GetIntArrayElements(leds, NULL);
        pthread_mutex_lock(&pwmLoopMutex);
        for (int i = 0; i < length; i++) {
            uint8_t led = (uint8_t)elems[i];

            if (led >= controls.size()) std::cerr << "Overflow" << std::endl;

            PulseControl& ctrl = controls[led];
            ctrl.initCommands(programs[(unsigned)program]);
            ctrl.enable();
            std::cerr << "Enabled program " << program << " for led " << (unsigned) led << std::endl;
        }
        pwmCtrlVersion++;
        std::cerr << "Program started" << std::endl;
        pthread_mutex_unlock(&pwmLoopMutex);
        env->ReleaseIntArrayElements(leds, elems, JNI_ABORT);
    }


}

}
#endif
