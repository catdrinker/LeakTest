# android 中的绘制流程

仅从`android`中来看的话，绘制流程是从`ViewRootImpl`的`mTraversalRunnable`被`Choreographer`调用`postCallback()`开始，其中，`input`,动画，和`draw`都会主动触发`vSync`请求，当前在主线程则直接调用，否则会被`post`到对应的`Looper`中处理后再发起`vSync`请求。

其中当native处理好`vSync`请求后会从`native`将`vSync`信号回调到`FrameDisplayEventReceiver`让其来处理`ui`。VSync信号由SurfaceFlinger实现并定时发送。FrameDisplayEventReceiver收到信号后，调用onVsync方法组织消息发送到主线程处理。

[Choreographer 工作原理](https://www.jianshu.com/p/996bca12eb1d)

### Choreographer 工作原理

先从`Choreographer`的构造函数开始分析：

```java
private Choreographer(Looper looper, int vsyncSource) {
        mLooper = looper;
        mHandler = new FrameHandler(looper);
        mDisplayEventReceiver = USE_VSYNC
                ? new FrameDisplayEventReceiver(looper, vsyncSource)
                : null;
        mLastFrameTimeNanos = Long.MIN_VALUE;

        mFrameIntervalNanos = (long)(1000000000 / getRefreshRate());

        mCallbackQueues = new CallbackQueue[CALLBACK_LAST + 1];
        for (int i = 0; i <= CALLBACK_LAST; i++) {
            mCallbackQueues[i] = new CallbackQueue();
        }
        // b/68769804: For low FPS experiments.
        setFPSDivisor(SystemProperties.getInt(ThreadedRenderer.DEBUG_FPS_DIVISOR, 1));
    }
```

可以看到传入了几个参数，其中有Looper是当前线程的循环器，`FrameHandler`则是一个`Handler`用来处理对应类型的消息。

`FrameDisplayEventReceiver`用来接收垂直同步脉冲，就是`VSync`信号，`VSync`信号是一个时间脉冲，一般为60HZ，用来控制系统同步操作，怎么同`ChoreoGrapher`一起工作的，将在下文介绍。

`mLastFrameTimeNanos`(标记上一个`frame`的渲染时间)以及`mFrameIntervalNanos`(帧率,fps，一般手机上为1s/60)。

`CallbackQueue`，`callback`队列，将在下一帧开始渲染时回调。

```java
private final class FrameHandler extends Handler {
        public FrameHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DO_FRAME:
                    doFrame(System.nanoTime(), 0);
                    break;
                case MSG_DO_SCHEDULE_VSYNC:
                    doScheduleVsync();
                    break;
                case MSG_DO_SCHEDULE_CALLBACK:
                    doScheduleCallback(msg.arg1);
                    break;
            }
        }
    }
```



### ViewRootImpl 如何处理ui

```java
final class TraversalRunnable implements Runnable {
        @Override
        public void run() {
            doTraversal();
        }
    }
```

![image-20200114170645119](/Users/panhao/Library/Application Support/typora-user-images/image-20200114170645119.png)

其实是在`scheduleTraversals`中调用了`mChoreographer.postCallback(Runnable)`，实际这里的`runnable`是回调被调用的。

```java
@UnsupportedAppUsage
    void scheduleTraversals() {
        if (!mTraversalScheduled) {
            mTraversalScheduled = true;
            mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();
            mChoreographer.postCallback(
                    Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
            if (!mUnbufferedInputDispatch) {
                scheduleConsumeBatchedInput();
            }
            notifyRendererOfFramePending();
            pokeDrawLockIfNeeded();
        }
    }
```

```java
private void postCallbackDelayedInternal(int callbackType,
            Object action, Object token, long delayMillis) {
        if (DEBUG_FRAMES) {
            Log.d(TAG, "PostCallback: type=" + callbackType
                    + ", action=" + action + ", token=" + token
                    + ", delayMillis=" + delayMillis);
        }

        synchronized (mLock) {
            final long now = SystemClock.uptimeMillis();
            final long dueTime = now + delayMillis;
            mCallbackQueues[callbackType].addCallbackLocked(dueTime, action, token);

            if (dueTime <= now) {
                scheduleFrameLocked(now);
            } else {
                Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_CALLBACK, action);
                msg.arg1 = callbackType;
                msg.setAsynchronous(true);
                mHandler.sendMessageAtTime(msg, dueTime);
            }
        }
    }
```

```java
public void addCallbackLocked(long dueTime, Object action, Object token) {
            CallbackRecord callback = obtainCallbackLocked(dueTime, action, token);
            CallbackRecord entry = mHead;
            if (entry == null) {
                mHead = callback;
                return;
            }
            if (dueTime < entry.dueTime) {
                callback.next = entry;
                mHead = callback;
                return;
            }
            while (entry.next != null) {
                if (dueTime < entry.next.dueTime) {
                    callback.next = entry.next;
                    break;
                }
                entry = entry.next;
            }
            entry.next = callback;
        }
```

可以看到这里会将`runnable`类型的消息封装成`CallbackRecord`加入到`mCallbackQueues`数组的`CALLBACK_TRAVERSAL`类型的消息队列中，不同`type`队列的优先级不一样，一般而言，`input`>`anim`>`draw`。

```java
private void scheduleFrameLocked(long now) {
        if (!mFrameScheduled) {
            mFrameScheduled = true;
            if (USE_VSYNC) {
                if (DEBUG_FRAMES) {
                    Log.d(TAG, "Scheduling next frame on vsync.");
                }

                // If running on the Looper thread, then schedule the vsync immediately,
                // otherwise post a message to schedule the vsync from the UI thread
                // as soon as possible.
                if (isRunningOnLooperThreadLocked()) {
                    scheduleVsyncLocked();
                } else {
                    Message msg = mHandler.obtainMessage(MSG_DO_SCHEDULE_VSYNC);
                    msg.setAsynchronous(true);
                    mHandler.sendMessageAtFrontOfQueue(msg);
                }
            } else {
                final long nextFrameTime = Math.max(
                        mLastFrameTimeNanos / TimeUtils.NANOS_PER_MS + sFrameDelay, now);
                if (DEBUG_FRAMES) {
                    Log.d(TAG, "Scheduling next frame in " + (nextFrameTime - now) + " ms.");
                }
                Message msg = mHandler.obtainMessage(MSG_DO_FRAME);
                msg.setAsynchronous(true);
                mHandler.sendMessageAtTime(msg, nextFrameTime);
            }
        }
    }

@UnsupportedAppUsage
    private void scheduleVsyncLocked() {
        mDisplayEventReceiver.scheduleVsync();
    }

@UnsupportedAppUsage
    public void scheduleVsync() {
        if (mReceiverPtr == 0) {
            Log.w(TAG, "Attempted to schedule a vertical sync pulse but the display event "
                    + "receiver has already been disposed.");
        } else {
            nativeScheduleVsync(mReceiverPtr);
        }
    }

@FastNative
    private static native void nativeScheduleVsync(long receiverPtr);
```

然后调用`scheduleFrameLocked`->`scheduleVsyncLocked`->`scheduleVsync`->`nativeScheduleVsync`，最后调用到`native`层的`scheduleVsync`发起调度`vSync`的请求。



发起请求后，在`DisplayEventReceiver`的`dispatchVsync`方法会在`native`调度处理好`vSync`信号后被调用

```java
// Called from native code.
    @SuppressWarnings("unused")
    @UnsupportedAppUsage
    private void dispatchVsync(long timestampNanos, long physicalDisplayId, int frame) {
        onVsync(timestampNanos, physicalDisplayId, frame);
    }
```

在`Choreographer`中`DisplayEventReceiver`的实例是`FrameDisplayEventReceiver`,其实现了父类的`onVsync`方法

```java
private final class FrameDisplayEventReceiver extends DisplayEventReceiver
            implements Runnable {
        private boolean mHavePendingVsync;
        private long mTimestampNanos;
        private int mFrame;

        public FrameDisplayEventReceiver(Looper looper, int vsyncSource) {
            super(looper, vsyncSource);
        }

        // TODO(b/116025192): physicalDisplayId is ignored because SF only emits VSYNC events for
        // the internal display and DisplayEventReceiver#scheduleVsync only allows requesting VSYNC
        // for the internal display implicitly.
        @Override
        public void onVsync(long timestampNanos, long physicalDisplayId, int frame) {
            // Post the vsync event to the Handler.
            // The idea is to prevent incoming vsync events from completely starving
            // the message queue.  If there are no messages in the queue with timestamps
            // earlier than the frame time, then the vsync event will be processed immediately.
            // Otherwise, messages that predate the vsync event will be handled first.
            long now = System.nanoTime();
            if (timestampNanos > now) {
                Log.w(TAG, "Frame time is " + ((timestampNanos - now) * 0.000001f)
                        + " ms in the future!  Check that graphics HAL is generating vsync "
                        + "timestamps using the correct timebase.");
                timestampNanos = now;
            }

            if (mHavePendingVsync) {
                Log.w(TAG, "Already have a pending vsync event.  There should only be "
                        + "one at a time.");
            } else {
                mHavePendingVsync = true;
            }

            mTimestampNanos = timestampNanos;
            mFrame = frame;
            Message msg = Message.obtain(mHandler, this);
            msg.setAsynchronous(true);
            mHandler.sendMessageAtTime(msg, timestampNanos / TimeUtils.NANOS_PER_MS);
        }

        @Override
        public void run() {
            mHavePendingVsync = false;
            doFrame(mTimestampNanos, mFrame);
        }
    }
```

实际上就是通过`FrameHandler`将自己这个`Runnable`作为消息插入到消息队列去，loop轮训到该条消息则调度自己的`run`方法，执行`doFrame`。

```java
void doFrame(long frameTimeNanos, int frame) {
        final long startNanos;
        synchronized (mLock) {
            if (!mFrameScheduled) {
                return; // no work to do
            }
          	...

        try {
            Trace.traceBegin(Trace.TRACE_TAG_VIEW, "Choreographer#doFrame");
            AnimationUtils.lockAnimationClock(frameTimeNanos / TimeUtils.NANOS_PER_MS);

            mFrameInfo.markInputHandlingStart();
            doCallbacks(Choreographer.CALLBACK_INPUT, frameTimeNanos);

            mFrameInfo.markAnimationsStart();
            doCallbacks(Choreographer.CALLBACK_ANIMATION, frameTimeNanos);
            doCallbacks(Choreographer.CALLBACK_INSETS_ANIMATION, frameTimeNanos);

            mFrameInfo.markPerformTraversalsStart();
            doCallbacks(Choreographer.CALLBACK_TRAVERSAL, frameTimeNanos);

            doCallbacks(Choreographer.CALLBACK_COMMIT, frameTimeNanos);
        } finally {
            AnimationUtils.unlockAnimationClock();
            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
        }

    }
```

可以看到，这里逐步针对`CALLBACK_INPUT`,`CALLBACK_ANIMATION`,`CALLBACK_INSETS_ANIMATION`,`CALLBACK_TRAVERSAL`和`CALLBACK_TRAVERSAL`的方法进行`doCallback`方式的处理。

```java
void doCallbacks(int callbackType, long frameTimeNanos) {
        CallbackRecord callbacks;
        synchronized (mLock) {
            
            final long now = System.nanoTime();
            callbacks = mCallbackQueues[callbackType].extractDueCallbacksLocked(
                    now / TimeUtils.NANOS_PER_MS);
            if (callbacks == null) {
                return;
            }
            mCallbacksRunning = true;
				...
        try {
            Trace.traceBegin(Trace.TRACE_TAG_VIEW, CALLBACK_TRACE_TITLES[callbackType]);
            for (CallbackRecord c = callbacks; c != null; c = c.next) {
              	// 找到传入的runnable, 调用它的run方法
                c.run(frameTimeNanos);
            }
        } finally {
            synchronized (mLock) {
                mCallbacksRunning = false;
                do {
                    final CallbackRecord next = callbacks.next;
                    recycleCallbackLocked(callbacks);
                    callbacks = next;
                } while (callbacks != null);
            }
            Trace.traceEnd(Trace.TRACE_TAG_VIEW);
        }
    }
```

```java
public CallbackRecord extractDueCallbacksLocked(long now) {
            CallbackRecord callbacks = mHead;
            if (callbacks == null || callbacks.dueTime > now) {
                return null;
            }

            CallbackRecord last = callbacks;
            CallbackRecord next = last.next;
            while (next != null) {
                if (next.dueTime > now) {
                    last.next = null;
                    break;
                }
                last = next;
                next = next.next;
            }
            mHead = next;
            return callbacks;
        }
```

可以看到，这里会取出对应`type`类型的`callback`，实际上就是当前type队列的`head`,按当前时间拆飞两个队列并重置`head`。最后拿着取出的这个`head`和它的队列遍历，逐条执行他们的`run`方法。

```java
private static final class CallbackRecord {
        public CallbackRecord next;
        public long dueTime;
        public Object action; // Runnable or FrameCallback
        public Object token;

        @UnsupportedAppUsage
        public void run(long frameTimeNanos) {
            if (token == FRAME_CALLBACK_TOKEN) {
                ((FrameCallback)action).doFrame(frameTimeNanos);
            } else {
                ((Runnable)action).run();
            }
        }
    }
```

因为我们一开始传入的`token`为`null`，最后也就会直接调用`Runnable`的`run`方法，也就是执行`TraversalRunnable`的`doTraversal`方法。

### 如何Measur Layout 和 Draw

因为回调到了`TraversalRunnable`的`run`方法，因此会执行到`doTraversal`方法，跟着会去执行`performTraversals`方法。



